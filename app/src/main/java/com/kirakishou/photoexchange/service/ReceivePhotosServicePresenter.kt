package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.interactors.ReceivePhotosUseCase
import com.kirakishou.photoexchange.mvp.model.FindPhotosData
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
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

open class ReceivePhotosServicePresenter(
  private val uploadedPhotosRepository: UploadedPhotosRepository,
  private val settingsRepository: SettingsRepository,
  private val receivePhotosUseCase: ReceivePhotosUseCase,
  private val dispatchersProvider: DispatchersProvider
) : CoroutineScope {
  private val TAG = "ReceivePhotosServicePresenter"

  private val compositeDisposable = CompositeDisposable()
  private val job = Job()

  val resultEventsSubject = PublishSubject.create<ReceivePhotoEvent>().toSerialized()
  var receiveActor: SendChannel<Unit>

  override val coroutineContext: CoroutineContext
    get() = job + dispatchersProvider.GENERAL()

  init {
    receiveActor = actor(capacity = 1) {
      consumeEach {
        try {
          receivePhotosInternal()
        } finally {
          sendEvent(ReceivePhotoEvent.StopService())
        }
      }
    }
  }

  private suspend fun receivePhotosInternal() {
    val photoData = formatRequestString()
    if (photoData == null) {
      return
    }

    sendEvent(ReceivePhotoEvent.OnNewNotification(NotificationType.Progress()))

    val receivedPhotos = try {
      receivePhotosUseCase.receivePhotos(photoData)
    } catch (error: Exception) {
      sendEvent(ReceivePhotoEvent.OnError(error))
      sendEvent(ReceivePhotoEvent.OnNewNotification(NotificationType.Error()))
      return
    }

    for ((receivedPhoto, takenPhotoName) in receivedPhotos) {
      sendEvent(ReceivePhotoEvent.OnReceivedPhoto(receivedPhoto, takenPhotoName))
    }

    sendEvent(ReceivePhotoEvent.OnNewNotification(NotificationType.Success()))
  }

  private suspend fun formatRequestString(): FindPhotosData? {
    val uploadedPhotos = uploadedPhotosRepository.findAllWithoutReceiverInfo()
    if (uploadedPhotos.isEmpty()) {
      Timber.tag(TAG).d("No photos without receiver info")
      return null
    }

    val photoNames = uploadedPhotos.joinToString(",") { it.photoName }
    val userId = settingsRepository.getUserId()
    if (userId.isEmpty()) {
      Timber.tag(TAG).d("UserId is empty")
      return null
    }

    return FindPhotosData(userId, photoNames)
  }

  private fun sendEvent(event: ReceivePhotoEvent) {
    resultEventsSubject.onNext(event)
  }

  fun onDetach() {
    job.cancel()
    compositeDisposable.clear()
  }

  fun observeResults(): Observable<ReceivePhotoEvent> {
    return resultEventsSubject
  }

  fun startPhotosReceiving() {
    Timber.tag(TAG).d("startPhotosReceiving called")
    receiveActor.offer(Unit)
  }

  sealed class ReceivePhotoEvent {
    class OnReceivedPhoto(val receivedPhoto: ReceivedPhoto,
                          val takenPhotoName: String) : ReceivePhotoEvent()
    class OnError(val error: Throwable) : ReceivePhotoEvent()
    class OnNewNotification(val type: NotificationType) : ReceivePhotoEvent()
    class RemoveNotification : ReceivePhotoEvent()
    class StopService : ReceivePhotoEvent()
  }

  sealed class NotificationType {
    class Progress : NotificationType()
    class Success : NotificationType()
    class Error : NotificationType()
  }
}