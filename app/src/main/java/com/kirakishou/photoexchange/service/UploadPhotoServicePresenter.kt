package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.interactors.GetUserIdUseCase
import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

/**
 * Created by kirakishou on 3/17/2018.
 */
open class UploadPhotoServicePresenter(
  private val takenPhotosRepository: TakenPhotosRepository,
  private val uploadPhotosUseCase: UploadPhotosUseCase,
  private val getUserIdUseCase: GetUserIdUseCase,
  private val dispatchersProvider: DispatchersProvider
) : CoroutineScope {
  private val TAG = "UploadPhotoServicePresenter"
  private val compositeDisposable = CompositeDisposable()
  private val job = Job()
  val resultEventsSubject = PublishSubject.create<UploadPhotoEvent>().toSerialized()

  private val uploadingActor: SendChannel<LonLat>
  private val eventsActor: SendChannel<UploadedPhotosFragmentEvent.PhotoUploadEvent>

  override val coroutineContext: CoroutineContext
    get() = job + dispatchersProvider.GENERAL()

  init {
    eventsActor = actor {
      consumeEach { event ->
        sendEvent(UploadPhotoEvent.UploadingEvent(event))
      }
    }

    uploadingActor = actor {
      consumeEach { location ->
        startUploading(location)
      }
    }
  }

  private suspend fun startUploading(location: LonLat) {
    updateServiceNotification(NotificationType.Uploading)

    try {
      val userId = getUserId()

      val hasErrors = doUploading(userId, location)
      if (!hasErrors) {
        updateServiceNotification(NotificationType.Success("All photos has been successfully uploaded"))
      } else {
        updateServiceNotification(NotificationType.Error("Could not upload one or more photos"))
      }
    } catch (error: Exception) {
      markAllPhotosAsFailed()
      updateServiceNotification(NotificationType.Error("Could not upload one or more photos"))
    } finally {
      sendEvent(UploadPhotoEvent.StopService)
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
      //send event on every photo
      eventsActor.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadStart(photo))

      try {
        uploadPhotosUseCase.uploadPhoto(photo, currentLocation, userId, eventsActor)
        eventsActor.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnUploaded(photo))
      } catch (error: Exception) {
        Timber.tag(TAG).e(error)

        hasErrors = true
        eventsActor.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnFailedToUpload(photo))
      }
    }

    eventsActor.send(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnEnd())
    return hasErrors
  }

  private fun sendEvent(event: UploadPhotoEvent) {
    resultEventsSubject.onNext(event)
  }

  private suspend fun markAllPhotosAsFailed() {
    takenPhotosRepository.updateStates(PhotoState.PHOTO_QUEUED_UP, PhotoState.FAILED_TO_UPLOAD)
    takenPhotosRepository.updateStates(PhotoState.PHOTO_UPLOADING, PhotoState.FAILED_TO_UPLOAD)
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

  fun getUserId(): String {
    return getUserIdUseCase.getUserId()
      .toObservable()
      .map { result ->
        if (result is Either.Error) {
          throw UploadPhotoServiceException.CouldNotGetUserIdException(result.error)
        }

        return@map (result as Either.Value).value
      }
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
    uploadingActor.offer(location)
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

  sealed class UploadPhotoServiceException : Exception() {
    class CouldNotGetUserIdException(val errorCode: ErrorCode) : UploadPhotoServiceException()
  }
}