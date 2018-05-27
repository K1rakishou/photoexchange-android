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
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.exception.CouldNotGetUserIdException
import com.kirakishou.photoexchange.mvp.model.exception.PhotoUploadingException
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.lang.ref.WeakReference

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
    private val uploadPhotosSubject = PublishSubject.create<Unit>().toSerialized()
    private val compositeDisposable = CompositeDisposable()

    init {
        compositeDisposable += uploadPhotosSubject
            .subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
            .concatMap {
                val currentLocationObservable = getCurrentLocation()
                val userIdObservable = getUserId()

                return@concatMap Observables.zip(currentLocationObservable, userIdObservable)
                    .concatMap { (currentLocation, userId) ->
                        Observable.fromCallable { takenPhotosRepository.findAllByState(PhotoState.PHOTO_QUEUED_UP) }
                            .flatMapSingle { queuedUpPhotos ->
                                return@flatMapSingle Observable.fromIterable(queuedUpPhotos)
                                    .concatMap { photo ->
                                        return@concatMap Observable.just(Unit)
                                            .doOnNext { sendEvent(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadStart(photo)) }
                                            .concatMap { uploadPhotosUseCase.uploadPhoto(photo, userId, currentLocation, callbacks) }
                                            .doOnNext { uploadedPhoto -> sendEvent(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnUploaded(uploadedPhoto)) }
                                            .doOnError { error -> handleRemoteErrors(photo, error) }
                                    }
                                    .toList()
                                    .map { uploadedPhotos -> uploadedPhotos.size == queuedUpPhotos.size }
                            }
                    }
                    .doOnNext { allUploaded -> sendEvent(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnEnd(allUploaded)) }
                    .doOnError { error -> handleUnknownErrors(error) }
                    .map { Unit }
                    .onErrorReturnItem(Unit)
            }
            .doOnEach { stopService() }
            .subscribe()
    }

    fun onDetach() {
        this.compositeDisposable.clear()
    }

    fun uploadPhotos() {
        Timber.tag(TAG).d("uploadPhotos called")
        uploadPhotosSubject.onNext(Unit)
    }

    private fun handleRemoteErrors(takenPhoto: TakenPhoto, error: Throwable) {
        if (error is PhotoUploadingException) {
            val errorCode = when (error) {
                is PhotoUploadingException.RemoteServerException -> error.remoteErrorCode
                is PhotoUploadingException.PhotoDoesNotExistOnDisk -> ErrorCode.UploadPhotoErrors.LocalNoPhotoFileOnDisk()
                is PhotoUploadingException.CouldNotRotatePhoto -> ErrorCode.UploadPhotoErrors.LocalDatabaseError()
                is PhotoUploadingException.DatabaseException -> ErrorCode.UploadPhotoErrors.CouldNotRotatePhoto()
            }

            sendEvent(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnFailedToUpload(takenPhoto, errorCode))
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
                //already handled upstream
            }

            else -> {
                callbacks.get()?.onUploadingEvent(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnUnknownError(error))
            }
        }

        callbacks.get()?.onError(error)
    }

    private fun getUserId(): Observable<String> {
        return Observable.fromCallable { settingsRepository.getUserId() }
            .concatMap { userId ->
                if (userId.isEmpty()) {
                    getUserIdUseCase.getUserId()
                        .toObservable()
                        .map { result ->
                            if (result is Either.Error) {
                                throw CouldNotGetUserIdException()
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

    private fun stopService() {
        callbacks.get()?.stopService()
    }

    private fun sendEvent(event: UploadedPhotosFragmentEvent.PhotoUploadEvent) {
        callbacks.get()?.onUploadingEvent(event)
    }
}