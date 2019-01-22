package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.helper.exception.EmptyUserUuidException
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.usecases.GetUserUuidUseCase
import com.kirakishou.photoexchange.usecases.UpdateFirebaseTokenUseCase
import com.kirakishou.photoexchange.usecases.UploadPhotosUseCase
import com.kirakishou.photoexchange.mvrx.model.PhotoState
import com.kirakishou.photoexchange.mvrx.model.photo.TakenPhoto
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

/**
 * Created by kirakishou on 3/17/2018.
 */
open class UploadPhotoServicePresenter(
  private val settingsRepository: SettingsRepository,
  private val takenPhotosRepository: TakenPhotosRepository,
  private val uploadPhotosUseCase: UploadPhotosUseCase,
  private val getUserUuidUseCase: GetUserUuidUseCase,
  private val updateFirebaseTokenUseCase: UpdateFirebaseTokenUseCase,
  private val dispatchersProvider: DispatchersProvider
) : CoroutineScope {
  private val TAG = "UploadPhotoServicePresenter"
  private val compositeDisposable = CompositeDisposable()
  private val job = Job()
  private val resultEventsSubject = PublishSubject.create<UploadPhotoEvent>().toSerialized()

  private val uploadingActor: SendChannel<Unit>
  private val eventsActor: SendChannel<UploadedPhotosFragmentEvent.PhotoUploadEvent>
  private val photosToCancel = hashSetOf<Long>()

  override val coroutineContext: CoroutineContext
    get() = job + dispatchersProvider.GENERAL()

  init {
    eventsActor = actor(capacity = Channel.UNLIMITED) {
      consumeEach { event ->
        sendEvent(UploadPhotoEvent.UploadingEvent(event))
      }
    }

    uploadingActor = actor(capacity = Channel.RENDEZVOUS) {
      consumeEach {
        try {
          startUploading()
        } finally {
          sendEvent(UploadPhotoEvent.StopService)
        }
      }
    }
  }

  private suspend fun startUploading() {
    Timber.tag(TAG).d("startUploading called")
    updateServiceNotification(NotificationType.Uploading)

    try {
      //we need to get the userUuid first because this operation will create a default account on the server
      val userUuid = getUserUuidUseCase.getUserUuid()
      if (userUuid.isEmpty()) {
        Timber.tag(TAG).d("UserUuid is empty")
        throw EmptyUserUuidException()
      }

      val token = settingsRepository.getFirebaseToken()
      if (token.isEmpty()) {
        Timber.tag(TAG).d("Firebase token is empty, trying to fetch new one")
        updateFirebaseTokenUseCase.updateFirebaseTokenIfNecessary()
      }

      val hasErrors = doUploading(userUuid)
      if (!hasErrors) {
        updateServiceNotification(NotificationType.Success("All photos has been successfully uploaded"))
      } else {
        updateServiceNotification(NotificationType.Error("Could not upload one or more photos"))
      }
    } catch (error: Exception) {
      Timber.tag(TAG).e(error)

      takenPhotosRepository.updateStates(PhotoState.PHOTO_UPLOADING, PhotoState.PHOTO_QUEUED_UP)
      updateServiceNotification(NotificationType.Error("Could not upload one or more photos"))

      eventsActor.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnError(error))
    }
  }

  private suspend fun doUploading(userUuid: String): Boolean {
    val queuedUpPhotos = takenPhotosRepository.findAllByState(PhotoState.PHOTO_QUEUED_UP)
    if (queuedUpPhotos.isEmpty()) {
      //should not really happen, since we make a check before starting the service

      Timber.tag(TAG).d("No queued up photos")
      return false
    }

    var hasErrors = false

    eventsActor.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingStart())

    for (photo in queuedUpPhotos) {
      Timber.tag(TAG).d("Uploading photo with id: ${photo.id} and name ${photo.photoName}")

      if (photosToCancel.contains(photo.id)) {
        Timber.tag(TAG).d("Photo uploading has been canceled for photo with id: ${photo.id} and name ${photo.photoName}")

        if (cancelPhotoUploading(photo)) {
          //if there were a photo in the database - notify the UI about it's deletion too
          eventsActor.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoCanceled(photo))
        }

        continue
      }

      try {
        val result = uploadPhotosUseCase.uploadPhoto(photo, userUuid, eventsActor)

        eventsActor.send(
          UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploaded(
            photo,
            result.photoId,
            result.photoName,
            result.uploadedOn
          )
        )

        Timber.tag(TAG).d("Successfully uploaded photo with id: ${photo.id} and name ${photo.photoName}")
      } catch (error: Exception) {
        Timber.tag(TAG).e(
          error,
          "Failed to upload photo  with id: ${photo.id} and name ${photo.photoName}"
        )

        hasErrors = true
        takenPhotosRepository.updatePhotoState(photo.id, PhotoState.PHOTO_QUEUED_UP)
        eventsActor.send(
          UploadedPhotosFragmentEvent.PhotoUploadEvent.OnFailedToUploadPhoto(
            photo,
            error
          )
        )
      }
    }

    Timber.tag(TAG).d("Done uploading photos")
    eventsActor.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnEnd())
    return hasErrors
  }

  /**
   * Returns true if photo existed in the database and was deleted
   * */
  private suspend fun cancelPhotoUploading(photo: TakenPhoto): Boolean {
    if (takenPhotosRepository.findById(photo.id) != null) {
      if (!takenPhotosRepository.deletePhotoById(photo.id)) {
        throw DatabaseException("Could not delete photo with name ${photo.photoName} and if ${photo.id}")
      }

      return true
    }

    photosToCancel.remove(photo.id)
    return false
  }

  private fun sendEvent(event: UploadPhotoEvent) {
    resultEventsSubject.onNext(event)
  }

  private fun updateServiceNotification(serviceNotification: NotificationType) {
    when (serviceNotification) {
      is UploadPhotoServicePresenter.NotificationType.Uploading -> {
        sendEvent(UploadPhotoEvent.OnNewNotification(NotificationType.Uploading))
      }
      is UploadPhotoServicePresenter.NotificationType.Success -> {
        sendEvent(UploadPhotoEvent.OnNewNotification(NotificationType.Success(serviceNotification.message)))
      }
      is UploadPhotoServicePresenter.NotificationType.Error -> {
        sendEvent(UploadPhotoEvent.OnNewNotification(NotificationType.Error(serviceNotification.errorMessage)))
      }
    }.safe
  }

  fun onDetach() {
    job.cancelChildren()
    compositeDisposable.clear()
  }

  fun observeResults(): Observable<UploadPhotoEvent> {
    return resultEventsSubject
  }

  fun uploadPhotos() {
    Timber.tag(TAG).d("uploadPhotos called")

    if (!uploadingActor.offer(Unit)) {
      Timber.tag(TAG).d("uploadingActor is busy")
    }
  }

  fun cancelPhotoUploading(photoId: Long) {
    photosToCancel.add(photoId)
  }

  sealed class UploadPhotoEvent {
    class UploadingEvent(val nestedEvent: UploadedPhotosFragmentEvent.PhotoUploadEvent) : UploadPhotoEvent()
    class OnNewNotification(val type: NotificationType) : UploadPhotoEvent()
    object RemoveNotification : UploadPhotoEvent()
    object StopService : UploadPhotoEvent()
  }

  sealed class NotificationType {
    object Uploading : NotificationType()
    class Success(val message: String) : NotificationType()
    class Error(val errorMessage: String) : NotificationType()
  }
}