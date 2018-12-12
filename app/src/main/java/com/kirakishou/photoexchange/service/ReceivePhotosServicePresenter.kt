package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.helper.Constants
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.interactors.ReceivePhotosUseCase
import com.kirakishou.photoexchange.mvp.model.FindPhotosData
import com.kirakishou.photoexchange.mvp.model.photo.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.photo.UploadedPhoto
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
    receiveActor = actor(capacity = Channel.RENDEZVOUS) {
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
    val uploadedPhotos = uploadedPhotosRepository.findAllWithoutReceiverInfo()
    if (uploadedPhotos.isEmpty()) {
      Timber.tag(TAG).d("No photos without receiver info")
      return
    }

    Timber.tag(TAG).d("Found ${uploadedPhotos.size} photos without receiver")

    val photoData = formatRequestString(uploadedPhotos)
    if (photoData == null) {
      return
    }

    sendEvent(ReceivePhotoEvent.OnNewNotification(NotificationType.Progress()))

    val receivedPhotos = try {
      receivePhotosUseCase.receivePhotos(photoData)
    } catch (error: Throwable) {
      Timber.tag(TAG).e(error)

      sendEvent(ReceivePhotoEvent.OnError(error))
      sendEvent(ReceivePhotoEvent.OnNewNotification(NotificationType.Error()))
      return
    }

    sendEvent(ReceivePhotoEvent.OnPhotosReceived(receivedPhotos))
    sendEvent(ReceivePhotoEvent.OnNewNotification(NotificationType.Success()))
  }

  private suspend fun formatRequestString(uploadedPhotos: List<UploadedPhoto>): FindPhotosData? {
    val photoNames = uploadedPhotos.joinToString(Constants.DELIMITER) { it.photoName }
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
    job.cancelChildren()
    compositeDisposable.clear()
  }

  fun observeResults(): Observable<ReceivePhotoEvent> {
    return resultEventsSubject
  }

  fun startPhotosReceiving() {
    Timber.tag(TAG).d("startPhotosReceiving called")

    if (!receiveActor.offer(Unit)) {
      Timber.tag(TAG).d("receiveActor is busy")
    }
  }

  sealed class ReceivePhotoEvent {
    class OnPhotosReceived(val receivedPhotos: List<ReceivedPhoto>) : ReceivePhotoEvent()
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