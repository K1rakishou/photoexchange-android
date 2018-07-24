package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.interactors.GetUserIdUseCase
import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.exception.PhotoUploadingException
import com.kirakishou.photoexchange.mvp.model.exception.UploadPhotoServiceException
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Created by kirakishou on 3/17/2018.
 */
open class UploadPhotoServicePresenter(
    private val takenPhotosRepository: TakenPhotosRepository,
    private val schedulerProvider: SchedulerProvider,
    private val uploadPhotosUseCase: UploadPhotosUseCase,
    private val getUserIdUseCase: GetUserIdUseCase,
    private val uploadPhotosDelayMs: Long
) {
    private val TAG = "UploadPhotoServicePresenter"
    private val compositeDisposable = CompositeDisposable()

    val uploadPhotosSubject = PublishSubject.create<LonLat>().toSerialized()
    val resultEventsSubject = PublishSubject.create<UploadPhotoEvent>().toSerialized()

    init {
        compositeDisposable += uploadPhotosSubject
            .subscribeOn(schedulerProvider.IO())
            .concatMap { location -> prepareForUploading(location) }
            .doOnEach { sendEvent(UploadPhotoEvent.StopService()) }
            .subscribe()
    }

    private fun prepareForUploading(location: LonLat): Observable<Unit> {
        return Observable.just(Unit)
            .concatMap {
                updateServiceNotification(NotificationType.Uploading())

                val currentLocationObservable = Observable.just(location)
                val userIdObservable = getUserId()

                return@concatMap Observables.zip(currentLocationObservable, userIdObservable)
                    .concatMap { (currentLocation, userId) -> doUploading(userId, currentLocation) }
            }
            .doOnNext { hasErrors ->
                if (!hasErrors) {
                    updateServiceNotification(NotificationType.Success("All photos has been successfully uploaded"))
                } else {
                    updateServiceNotification(NotificationType.Error("Could not upload one or more photos"))
                }
            }
            .doOnError { error ->
                markAllPhotosAsFailed()
                updateServiceNotification(NotificationType.Error("Could not upload one or more photos"))
                handleErrors(error)
            }
            .map { Unit }
            .onErrorReturnItem(Unit)
    }

    private fun doUploading(userId: String, currentLocation: LonLat): Observable<Boolean> {
        val queuedUpPhotos = takenPhotosRepository.findAllByState(PhotoState.PHOTO_QUEUED_UP)
        if (queuedUpPhotos.isEmpty()) {
            //should not really happen, since we make a check before starting the service
            return Observable.just(false)
        }

        return Observable.fromIterable(queuedUpPhotos)
            .concatMap { photo -> startUploadingInternal(photo, userId, currentLocation) }
            .toList()
            .map { results -> results.none { result -> !result } }
            //1 second delay before starting to upload the next photo
            .delay(uploadPhotosDelayMs, TimeUnit.MILLISECONDS, schedulerProvider.CALC())
            .toObservable()
            //send event when all photos has been uploaded without errors
            .doOnNext {
                sendEvent(UploadPhotoEvent.UploadingEvent(
                    UploadedPhotosFragmentEvent.PhotoUploadEvent.OnEnd())
                )
            }
    }

    private fun startUploadingInternal(photo: TakenPhoto, userId: String, currentLocation: LonLat): Observable<Boolean>? {
        //send event on every photo
        sendEvent(UploadPhotoEvent.UploadingEvent(
            UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadStart(photo))
        )

        return uploadOnePhoto(photo, userId, currentLocation)
            .doOnNext { hasErrors ->
                if (!hasErrors) {
                    sendEvent(UploadPhotoEvent.UploadingEvent(
                        UploadedPhotosFragmentEvent.PhotoUploadEvent.OnUploaded(photo))
                    )
                }
            }
    }

    private fun uploadOnePhoto(photo: TakenPhoto, userId: String, currentLocation: LonLat): Observable<Boolean> {
        return Observable.just(Unit)
            .concatMap {
                return@concatMap uploadPhotosUseCase.uploadPhoto(photo, userId, currentLocation)
                    .onErrorReturn { error -> UploadedPhotosFragmentEvent.PhotoUploadEvent.OnUnknownError(error) }
            }
            .doOnNext { event -> handleEvents(photo, event) }
            .toList()
            .map(this::hasErrorEvents)
            .toObservable()
    }

    private fun hasErrorEvents(eventsList: List<UploadedPhotosFragmentEvent.PhotoUploadEvent>): Boolean {
        return eventsList.any {
            return@any (it is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnKnownError) ||
                (it is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnUnknownError)
        }
    }

    private fun handleEvents(takenPhoto: TakenPhoto, event: UploadedPhotosFragmentEvent.PhotoUploadEvent) {
        if (event !is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnKnownError &&
            event !is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnUnknownError) {
            sendEvent(UploadPhotoEvent.UploadingEvent(event))
            return
        }

        when (event) {
            is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnKnownError -> {
                markPhotoAsFailed(takenPhoto)
                sendEvent(UploadPhotoEvent.UploadingEvent(
                    UploadedPhotosFragmentEvent.PhotoUploadEvent.OnFailedToUpload(takenPhoto, event.errorCode))
                )
            }
            is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnUnknownError -> {
                Timber.tag(TAG).e(event.error)

                markPhotoAsFailed(takenPhoto)
                sendEvent(UploadPhotoEvent.UploadingEvent(
                    UploadedPhotosFragmentEvent.PhotoUploadEvent.OnFailedToUpload(takenPhoto, ErrorCode.UploadPhotoErrors.UnknownError()))
                )
            }
            else -> throw IllegalArgumentException("Unknown event ${event::class.java}")
        }
    }

    private fun handleErrors(error: Throwable) {
        Timber.tag(TAG).e(error)

        when (error) {
            is PhotoUploadingException.ApiException,
            is PhotoUploadingException.PhotoDoesNotExistOnDisk,
            is PhotoUploadingException.CouldNotRotatePhoto,
            is PhotoUploadingException.DatabaseException -> {
                //already handled by upstream
            }

            is UploadPhotoServiceException.CouldNotGetUserIdException -> {
                sendEvent(UploadPhotoEvent.UploadingEvent(UploadedPhotosFragmentEvent.PhotoUploadEvent
                    .OnKnownError(ErrorCode.UploadPhotoErrors.LocalCouldNotGetUserId())))
            }

            else -> {
                sendEvent(UploadPhotoEvent.UploadingEvent(UploadedPhotosFragmentEvent.PhotoUploadEvent
                    .OnUnknownError(error)))
            }
        }.safe
    }

    private fun sendEvent(event: UploadPhotoEvent) {
        resultEventsSubject.onNext(event)
    }

    private fun markPhotoAsFailed(takenPhoto: TakenPhoto) {
        takenPhotosRepository.updatePhotoState(takenPhoto.id, PhotoState.FAILED_TO_UPLOAD)
    }

    private fun markAllPhotosAsFailed() {
        takenPhotosRepository.updateStates(PhotoState.PHOTO_QUEUED_UP, PhotoState.FAILED_TO_UPLOAD)
        takenPhotosRepository.updateStates(PhotoState.PHOTO_UPLOADING, PhotoState.FAILED_TO_UPLOAD)
    }

    private fun updateServiceNotification(serviceNotification: NotificationType) {
        when (serviceNotification) {
            is UploadPhotoServicePresenter.NotificationType.Uploading -> {
                sendEvent(UploadPhotoEvent.OnNewNotification(NotificationType.Uploading()))
            }
            is UploadPhotoServicePresenter.NotificationType.Success -> {
                sendEvent(UploadPhotoEvent.OnNewNotification(NotificationType.Success(serviceNotification.message)))
            }
            is UploadPhotoServicePresenter.NotificationType.Error -> {
                sendEvent(UploadPhotoEvent.OnNewNotification(NotificationType.Error(serviceNotification.errorMessage)))
            }
        }.safe
    }

    fun getUserId(): Observable<String> {
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
        this.compositeDisposable.clear()
    }

    fun observeResults(): Observable<UploadPhotoEvent> {
        return resultEventsSubject
    }

    fun uploadPhotos(location: LonLat) {
        Timber.tag(TAG).d("uploadPhotos called")
        uploadPhotosSubject.onNext(location)
    }

    sealed class UploadPhotoEvent {
        class UploadingEvent(val nestedEvent: UploadedPhotosFragmentEvent.PhotoUploadEvent) : UploadPhotoEvent()
        class OnNewNotification(val type: NotificationType) : UploadPhotoEvent()
        class RemoveNotification : UploadPhotoEvent()
        class StopService : UploadPhotoEvent()
    }

    sealed class NotificationType {
        class Uploading : NotificationType()
        class Success(val message: String) : NotificationType()
        class Error(val errorMessage: String) : NotificationType()
    }
}