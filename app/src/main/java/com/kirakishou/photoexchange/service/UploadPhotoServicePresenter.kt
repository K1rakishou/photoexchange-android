package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.interactors.GetUserIdUseCase
import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.zipWith
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 3/17/2018.
 */
class UploadPhotoServicePresenter(
    private val callbacks: WeakReference<UploadPhotoServiceCallbacks>,
    private val myTakenPhotosRepository: TakenPhotosRepository,
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
            .flatMap {
                return@flatMap getCurrentLocation()
                    .flatMap { location -> getUserId().zipWith(Observable.just(location)) }
                    .flatMap { (userIdResult, location) ->
                        if (userIdResult is Either.Error) {
                            callbacks.get()?.onUploadingEvent(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnCouldNotGetUserIdFromServerError(
                                userIdResult.error as ErrorCode.UploadPhotoErrors))
                            return@flatMap Observable.error<Pair<String, LonLat>>(CouldNotGetUserIdFromServerException())
                        }

                        return@flatMap Observable.just(Pair((userIdResult as Either.Value).value, location))
                    }
                    .doOnNext { callbacks.get()?.onUploadingEvent(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPrepare()) }
                    .concatMap { (userId, location) ->
                        Timber.tag(TAG).d("Upload data")

                        return@concatMap uploadPhotosUseCase.uploadPhotos(userId, location, callbacks)
                            .toObservable()
                    }
                    .doOnNext { allUploaded ->
                        Timber.tag(TAG).d("onUploadingEvent(PhotoUploadEvent.OnEnd($allUploaded))")
                        callbacks.get()?.onUploadingEvent(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnEnd(allUploaded))
                    }
                    .doOnNext {
                        Timber.tag(TAG).d("stopService")
                        callbacks.get()?.stopService()
                    }
                    .doOnError { error ->
                        Timber.tag(TAG).d("onUploadingEvent(PhotoUploadEvent.OnEnd())")

                        markAllPhotosAsFailed()

                        when (error) {
                            is CouldNotGetUserIdFromServerException -> {
                            }

                            else -> callbacks.get()?.onUploadingEvent(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnUnknownError(error))
                        }

                        callbacks.get()?.onError(error)
                    }
                    .map { Unit }
                    .onErrorReturnItem(Unit)
            }
            .subscribe()
    }

    private fun markAllPhotosAsFailed() {
        myTakenPhotosRepository.updateStates(PhotoState.PHOTO_QUEUED_UP, PhotoState.FAILED_TO_UPLOAD)
        myTakenPhotosRepository.updateStates(PhotoState.PHOTO_UPLOADING, PhotoState.FAILED_TO_UPLOAD)
    }

    private fun getUserId(): Observable<Either<ErrorCode, String>> {
        return Observable.fromCallable { settingsRepository.getUserId() }
            .concatMap { userId ->
                if (userId.isEmpty()) {
                    getUserIdUseCase.getUserId()
                        .toObservable()
                } else {
                    Observable.just(Either.Value(userId))
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

    fun uploadPhotos() {
        Timber.tag(TAG).d("uploadPhotos called")
        uploadPhotosSubject.onNext(Unit)
    }

    class CouldNotGetUserIdFromServerException() : Exception()
}