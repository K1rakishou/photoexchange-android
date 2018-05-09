package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.interactors.GetUserIdUseCase
import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.PhotoUploadEvent
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
    private val myPhotosRepository: PhotosRepository,
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
            .flatMap { getCurrentLocation() }
            .flatMap { location -> getUserId().zipWith(Observable.just(location)) }
            .map { (userIdResult, location) ->
                if (userIdResult is Either.Error) {
                    callbacks.get()?.onUploadingEvent(PhotoUploadEvent.OnCouldNotGetUserIdFromUserver(userIdResult.error as ErrorCode.UploadPhotoErrors))
                    throw StopUploadingException()
                }

                return@map Pair((userIdResult as Either.Value).value, location)
            }
            .doOnNext { callbacks.get()?.onUploadingEvent(PhotoUploadEvent.OnPrepare()) }
            .concatMap { (userId, location) ->
                Timber.tag(TAG).d("Upload data")

                return@concatMap uploadPhotosUseCase.uploadPhotos(userId, location, callbacks)
                    .toObservable()
            }
            .doOnNext { allUploaded ->
                Timber.tag(TAG).d("onUploadingEvent(PhotoUploadEvent.OnEnd($allUploaded))")
                callbacks.get()?.onUploadingEvent(PhotoUploadEvent.OnEnd(allUploaded))
            }
            .doOnError { error ->
                Timber.tag(TAG).d("onUploadingEvent(PhotoUploadEvent.OnEnd())")

                markAllPhotosAsFailed()
                callbacks.get()?.onUploadingEvent(PhotoUploadEvent.OnUnknownError(error))
                callbacks.get()?.onError(error)
            }
            .doOnEach {
                Timber.tag(TAG).d("stopService")
                callbacks.get()?.stopService()
            }
            .subscribe()
    }

    private fun markAllPhotosAsFailed() {
        myPhotosRepository.updateStates(PhotoState.PHOTO_QUEUED_UP, PhotoState.FAILED_TO_UPLOAD)
        myPhotosRepository.updateStates(PhotoState.PHOTO_UPLOADING, PhotoState.FAILED_TO_UPLOAD)
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
        val gpsGrantedObservable = Observable.fromCallable {
            settingsRepository.isGpsPermissionGranted()
        }

        val gpsGranted = gpsGrantedObservable
            .filter { it }
            .doOnNext { callbacks.get()?.onUploadingEvent(PhotoUploadEvent.OnLocationUpdateStart()) }
            .doOnNext { Timber.tag(TAG).d("Gps permission is granted") }
            .flatMap {
                callbacks.get()?.getCurrentLocation()?.toObservable()
                    ?: Observable.just(LonLat.empty())
            }
            .doOnNext { callbacks.get()?.onUploadingEvent(PhotoUploadEvent.OnLocationUpdateEnd()) }

        val gpsNotGranted = gpsGrantedObservable
            .filter { !it }
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

    class StopUploadingException() : Exception()
}