package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.interactors.GetUserIdUseCase
import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.helper.exception.EmptyUserIdException
import com.kirakishou.photoexchange.interactors.UpdateFirebaseTokenUseCase
import com.kirakishou.photoexchange.mvp.model.photo.TakenPhoto
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
  private val getUserIdUseCase: GetUserIdUseCase,
  private val updateFirebaseTokenUseCase: UpdateFirebaseTokenUseCase,
  private val dispatchersProvider: DispatchersProvider
) : CoroutineScope {
  private val TAG = "UploadPhotoServicePresenter"
  private val compositeDisposable = CompositeDisposable()
  private val job = Job()
  val resultEventsSubject = PublishSubject.create<UploadPhotoEvent>().toSerialized()

  private val uploadingActor: SendChannel<LonLat>
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
      consumeEach { location ->
        try {
          startUploading(location)
        } finally {
          sendEvent(UploadPhotoEvent.StopService)
        }
      }
    }
  }

  private suspend fun startUploading(location: LonLat) {
    Timber.tag(TAG).d("startUploading called")
    updateServiceNotification(NotificationType.Uploading)

    try {
      //we need to get the userId first because this operation will create a default account on the server
      val userId = getUserId()
      if (userId.isEmpty()) {
        throw EmptyUserIdException()
      }

      val token = settingsRepository.getFirebaseToken()
      if (token.isEmpty()) {
        updateFirebaseToken()
      }

      val hasErrors = doUploading(userId, location)
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

  private suspend fun doUploading(userId: String, currentLocation: LonLat): Boolean {
    val queuedUpPhotos = takenPhotosRepository.findAllByState(PhotoState.PHOTO_QUEUED_UP)
    if (queuedUpPhotos.isEmpty()) {
      //should not really happen, since we make a check before starting the service
      return false
    }

    var hasErrors = false

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

      //send event on every photo
      eventsActor.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingStart(photo))

      try {
        val result = uploadPhotosUseCase.uploadPhoto(photo, currentLocation, userId, eventsActor)
        eventsActor.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploaded(
          photo,
          result.photoId,
          result.photoName,
          result.uploadedOn,
          currentLocation)
        )

        Timber.tag(TAG).d("Successfully uploaded photo with id: ${photo.id} and name ${photo.photoName}")
      } catch (error: Exception) {
        Timber.tag(TAG).e("Failed to upload photo  with id: ${photo.id} and name ${photo.photoName}", error)

        hasErrors = true
        takenPhotosRepository.updatePhotoState(photo.id, PhotoState.PHOTO_QUEUED_UP)
        eventsActor.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnFailedToUploadPhoto(photo))
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

  private suspend fun updateFirebaseToken() {
    val result = updateFirebaseTokenUseCase.updateFirebaseTokenIfNecessary()

    when (result) {
      is Either.Value -> {
        //do nothing
      }
      is Either.Error -> throw result.error
    }.safe
  }

  private suspend fun getUserId(): String {
    val result = getUserIdUseCase.getUserId()

    when (result) {
      is Either.Value -> return result.value
      is Either.Error -> throw result.error
    }.safe
  }

  fun onDetach() {
    job.cancel()
    compositeDisposable.clear()
  }

  fun observeResults(): Observable<UploadPhotoEvent> {
    return resultEventsSubject
  }

  fun uploadPhotos(location: LonLat) {
    Timber.tag(TAG).d("uploadPhotos called")

    if (!uploadingActor.offer(location)) {
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