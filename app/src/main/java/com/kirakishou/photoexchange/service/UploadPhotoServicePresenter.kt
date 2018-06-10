package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
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
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

/**
 * Created by kirakishou on 3/17/2018.
 */
class UploadPhotoServicePresenter(
    private val callbacks: WeakReference<UploadPhotoServiceCallbacks>,
    private val takenPhotosRepository: TakenPhotosRepository,
    private val settingsRepository: SettingsRepository,
    private val schedulerProvider: SchedulerProvider,
    private val uploadPhotosUseCase: UploadPhotosUseCase,
    private val getUserIdUseCase: GetUserIdUseCase
) {
    private val TAG = "UploadPhotoServicePresenter"
    private val compositeDisposable = CompositeDisposable()

    val uploadPhotosSubject = PublishSubject.create<Unit>().toSerialized()
    val resultEventsSubject = PublishSubject.create<UploadPhotoEvent>().toSerialized()

    init {
        compositeDisposable += uploadPhotosSubject
            .subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
            .concatMap {
                val currentLocationObservable = getCurrentLocation()
                val userIdObservable = getUserId()

                return@concatMap Observable.fromCallable {
                    updateServiceNotification(NotificationType.Uploading())
                }
                    .flatMap { Observables.zip(currentLocationObservable, userIdObservable) }
                    .concatMap { (currentLocation, userId) ->
                        return@concatMap Observable.fromCallable { takenPhotosRepository.findAllByState(PhotoState.PHOTO_QUEUED_UP) }
                            .concatMapSingle { queuedUpPhotos ->
                                return@concatMapSingle Observable.fromIterable(queuedUpPhotos)
                                    .concatMap { photo ->
                                        return@concatMap Observable.just(Unit)
                                            .doOnNext {
                                                sendEvent(UploadPhotoEvent.UploadingEvent(
                                                    UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadStart(photo))
                                                )
                                            }
                                            .concatMap {
                                                return@concatMap Observable.create<UploadedPhotosFragmentEvent.PhotoUploadEvent> { emitter ->
                                                    uploadPhotosUseCase.uploadPhoto(photo, userId, currentLocation, emitter)
                                                }
                                            }
                                            .doOnNext { event -> sendEvent(UploadPhotoEvent.UploadingEvent(event)) }
                                            .doOnError { error -> handleRemoteErrors(photo, error) }
                                    }
                                    .lastOrError()
                                    //1 second delay before starting to upload the next photo
                                    .delay(1, TimeUnit.SECONDS)
                                    .map { true }
                            }
                    }
                    .doOnNext { allUploaded ->
                        updateServiceNotification(NotificationType.Success("All photos has been successfully uploaded"))
                        sendEvent(UploadPhotoEvent.UploadingEvent(
                            UploadedPhotosFragmentEvent.PhotoUploadEvent.OnEnd(allUploaded))
                        )
                    }
                    .doOnError { error ->
                        updatePhotosStates()
                        updateServiceNotification(NotificationType.Error("Could not upload one or more photos"))
                        handleUnknownErrors(error)
                    }
                    .map { Unit }
                    .onErrorReturnItem(Unit)
            }
            .doOnEach { sendEvent(UploadPhotoEvent.StopService()) }
            .subscribe()
    }

    private fun sendEvent(event: UploadPhotoEvent) {
        resultEventsSubject.onNext(event)
    }

    private fun updatePhotosStates() {
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
        }
    }

    private fun handleRemoteErrors(takenPhoto: TakenPhoto, error: Throwable) {
        if (error is PhotoUploadingException) {
            val errorCode = when (error) {
                is PhotoUploadingException.RemoteServerException -> error.remoteErrorCode
                is PhotoUploadingException.PhotoDoesNotExistOnDisk -> ErrorCode.UploadPhotoErrors.LocalNoPhotoFileOnDisk()
                is PhotoUploadingException.CouldNotRotatePhoto -> ErrorCode.UploadPhotoErrors.LocalDatabaseError()
                is PhotoUploadingException.DatabaseException -> ErrorCode.UploadPhotoErrors.CouldNotRotatePhoto()
            }

            sendEvent(UploadPhotoEvent.UploadingEvent(
                UploadedPhotosFragmentEvent.PhotoUploadEvent.OnFailedToUpload(takenPhoto, errorCode))
            )
        }

        takenPhotosRepository.updatePhotoState(takenPhoto.id, PhotoState.FAILED_TO_UPLOAD)
    }

    private fun handleUnknownErrors(error: Throwable) {
        Timber.tag(TAG).d("onUploadingEvent(PhotoUploadEvent.OnEnd())")

        when (error) {
            is PhotoUploadingException.RemoteServerException,
            is PhotoUploadingException.PhotoDoesNotExistOnDisk,
            is PhotoUploadingException.CouldNotRotatePhoto,
            is PhotoUploadingException.DatabaseException -> {
                //already handled by upstream
            }

            is UploadPhotoServiceException.CouldNotGetUserIdException -> {
                val cause = when (error.errorCode) {
                    is ErrorCode.GetUserIdError.LocalTimeout -> ErrorCode.UploadPhotoErrors.LocalTimeout()
                    else -> ErrorCode.UploadPhotoErrors.UnknownError()
                }

                sendEvent(UploadPhotoEvent.UploadingEvent(UploadedPhotosFragmentEvent.knownError(cause)))
            }

            else -> {
                sendEvent(UploadPhotoEvent.UploadingEvent(UploadedPhotosFragmentEvent.unknownError(error)))
            }
        }
    }

    fun getUserId(): Observable<String> {
        return Observable.fromCallable { settingsRepository.getUserId() }
            .concatMap { userId ->
                if (userId.isEmpty()) {
                    getUserIdUseCase.getUserId()
                        .toObservable()
                        .map { result ->
                            if (result is Either.Error) {
                                throw UploadPhotoServiceException.CouldNotGetUserIdException(result.error)
                            }

                            return@map (result as Either.Value).value
                        }
                } else {
                    Observable.just(userId)
                }
            }
            .doOnError { Timber.tag(TAG).e(it) }
    }

    private fun getCurrentLocation(): Observable<LonLat> {
        val gpsPermissionGrantedObservable = Observable.fromCallable {
            settingsRepository.isGpsPermissionGranted()
        }

        val gpsGranted = gpsPermissionGrantedObservable
            .filter { permissionGranted -> permissionGranted }
            .doOnNext { Timber.tag(TAG).d("Gps permission is granted") }
            .flatMap {
                callbacks.get()?.getCurrentLocation()?.toObservable()
                    ?: Observable.just(LonLat.empty())
            }

        val gpsNotGranted = gpsPermissionGrantedObservable
            .filter { permissionGranted -> !permissionGranted }
            .doOnNext { Timber.tag(TAG).d("Gps permission is not granted") }
            .map { LonLat.empty() }

        return Observable.merge(gpsGranted, gpsNotGranted)
    }

    fun onDetach() {
        this.compositeDisposable.clear()
    }

    fun observeResults(): Observable<UploadPhotoEvent> {
        return resultEventsSubject
    }

    fun uploadPhotos() {
        Timber.tag(TAG).d("uploadPhotos called")
        uploadPhotosSubject.onNext(Unit)
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