package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.interactors.ReceivePhotosUseCase
import com.kirakishou.photoexchange.mvp.model.FindPhotosData
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.exception.ReceivePhotosServiceException
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit

open class ReceivePhotosServicePresenter(
    private val uploadedPhotosRepository: UploadedPhotosRepository,
    private val settingsRepository: SettingsRepository,
    private val schedulerProvider: SchedulerProvider,
    private val receivePhotosUseCase: ReceivePhotosUseCase
) {

    private val TAG = "ReceivePhotosServicePresenter"
    private val compositeDisposable = CompositeDisposable()

    val findPhotosSubject = PublishSubject.create<Unit>().toSerialized()
    val resultEventsSubject = PublishSubject.create<ReceivePhotoEvent>().toSerialized()

    init {
        compositeDisposable += findPhotosSubject
            .subscribeOn(schedulerProvider.IO())
            .delay(1, TimeUnit.SECONDS, schedulerProvider.CALC())
            .concatMap { receivePhotosInternal() }
            .doOnEach { sendEvent(ReceivePhotoEvent.StopService()) }
            .subscribe()
    }

    private fun receivePhotosInternal(): Observable<Unit> {
        return Observable.just(1)
            .doOnNext { sendEvent(ReceivePhotoEvent.OnNewNotification(NotificationType.Progress())) }
            .concatMap { formatRequestString() }
            .concatMapSingle { photoData ->
                return@concatMapSingle receivePhotosUseCase.receivePhotos(photoData)
                    .doOnNext { event -> handlePhotoReceivedEvent(event) }
                    .toList()
                    .map { hasErrorEvents(it) }
                    .doOnSuccess { hasErrors ->
                        if (!hasErrors) {
                            sendEvent(ReceivePhotoEvent.OnNewNotification(NotificationType.Success()))
                        } else {
                            sendEvent(ReceivePhotoEvent.OnNewNotification(NotificationType.Error()))
                        }
                    }
            }
            .doOnError { error -> onError(error) }
            .map { Unit }
            .onErrorReturnItem(Unit)
    }

    private fun hasErrorEvents(eventList: List<ReceivePhotoEvent>): Boolean {
        return eventList.any { it is ReceivePhotoEvent.OnKnownError || it is ReceivePhotoEvent.OnUnknownError }
    }

    private fun formatRequestString(): Observable<FindPhotosData> {
        val uploadedPhotos = uploadedPhotosRepository.findAllWithoutReceiverInfo()
        if (uploadedPhotos.isEmpty()) {
            return Observable.error<FindPhotosData>(ReceivePhotosServiceException.NoUploadedPhotosWithoutReceiverInfo())
        }

        val photoNames = uploadedPhotos.joinToString(",") { it.photoName }
        val userId = settingsRepository.getUserId()
        if (userId.isEmpty()) {
            return Observable.error<FindPhotosData>(ReceivePhotosServiceException.CouldNotGetUserId())
        }

        return Observable.just(FindPhotosData(userId, photoNames))
    }

    private fun onError(error: Throwable) {
        Timber.tag(TAG).e(error)

        if (handleErrors(error)) {
            sendEvent(ReceivePhotoEvent.OnNewNotification(NotificationType.Error()))
        } else {
            sendEvent(ReceivePhotoEvent.RemoveNotification())
        }
    }

    //returns true if we should show error notification
    private fun handleErrors(error: Throwable): Boolean {
        Timber.tag(TAG).e(error)

        if (error is ReceivePhotosServiceException) {
            val errorCode = when (error) {
                is ReceivePhotosServiceException.CouldNotGetUserId -> ErrorCode.ReceivePhotosErrors.LocalCouldNotGetUserId()
                is ReceivePhotosServiceException.ApiException -> error.remoteErrorCode
                is ReceivePhotosServiceException.PhotoNamesAreEmpty -> null
                is ReceivePhotosServiceException.NoUploadedPhotosWithoutReceiverInfo -> null
            }

            sendEvent(ReceivePhotoEvent.OnKnownError(errorCode))
            return errorCode != null
        }

        sendEvent(ReceivePhotoEvent.OnUnknownError(error))
        return true
    }

    private fun handlePhotoReceivedEvent(event: ReceivePhotoEvent) {
        when (event) {
            is ReceivePhotosServicePresenter.ReceivePhotoEvent.OnReceivedPhoto -> {
                sendEvent(ReceivePhotoEvent.OnReceivedPhoto(event.receivedPhoto, event.takenPhotoName))
            }
            is ReceivePhotosServicePresenter.ReceivePhotoEvent.OnKnownError -> {
                sendEvent(ReceivePhotoEvent.OnKnownError(event.errorCode))
            }
            is ReceivePhotosServicePresenter.ReceivePhotoEvent.OnUnknownError -> {
                sendEvent(ReceivePhotoEvent.OnUnknownError(event.error))
            }
            else -> throw IllegalStateException("Should not happen, event is $event")
        }.safe
    }

    private fun sendEvent(event: ReceivePhotoEvent) {
        resultEventsSubject.onNext(event)
    }

    fun onDetach() {
        this.compositeDisposable.clear()
    }

    fun observeResults(): Observable<ReceivePhotoEvent> {
        return resultEventsSubject
    }

    fun startPhotosReceiving() {
        Timber.tag(TAG).d("startPhotosReceiving called")
        findPhotosSubject.onNext(Unit)
    }

    sealed class ReceivePhotoEvent {
        class OnReceivedPhoto(val receivedPhoto: ReceivedPhoto,
                              val takenPhotoName: String) : ReceivePhotoEvent()
        class OnKnownError(val errorCode: ErrorCode.ReceivePhotosErrors?) : ReceivePhotoEvent()
        class OnUnknownError(val error: Throwable) : ReceivePhotoEvent()
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