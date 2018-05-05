package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.PhotoUploadEvent
import com.kirakishou.photoexchange.mvp.model.UploadPhotoData
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
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
    private val updatePhotosUseCase: UploadPhotosUseCase
) {
    private val TAG = "UploadPhotoServicePresenter"
    private val uploadPhotosSubject = PublishSubject.create<Unit>().toSerialized()
    private val compositeDisposable = CompositeDisposable()

    init {
        compositeDisposable += uploadPhotosSubject
            .subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
            .flatMap { getCurrentLocation() }
            .map { location ->
                val userId = settingsRepository.getUserId()
                    ?: return@map UploadPhotoData.empty()

                Timber.tag(TAG).d("userId = $userId, location = $location")
                return@map UploadPhotoData(false, userId, location)
            }
            .doOnError { error ->
                callbacks.get()?.onError(error)
                callbacks.get()?.stopService()
            }
            .doOnNext { data ->
                Timber.tag(TAG).d("Check if data is empty")
                if (data.isEmpty()) {
                    callbacks.get()?.stopService()
                }
            }
            .filter { data -> !data.isEmpty() }
            .doOnEach { callbacks.get()?.onUploadingEvent(PhotoUploadEvent.OnPrepare()) }
            .concatMap { data ->
                Timber.tag(TAG).d("Upload data")

                return@concatMap updatePhotosUseCase.uploadPhotos(data.userId, data.location, callbacks)
                    .toObservable()
            }
            .doOnNext { allUploaded ->
                Timber.tag(TAG).d("onUploadingEvent(PhotoUploadEvent.OnEnd($allUploaded))")
                callbacks.get()?.onUploadingEvent(PhotoUploadEvent.OnEnd(allUploaded))
            }
            .doOnError { error ->
                Timber.tag(TAG).d("onUploadingEvent(PhotoUploadEvent.OnEnd())")
                callbacks.get()?.onUploadingEvent(PhotoUploadEvent.OnUnknownError(error))
                callbacks.get()?.onError(error)
            }
            .doOnEach {
                Timber.tag(TAG).d("stopService")
                callbacks.get()?.stopService()
            }
            .subscribe()
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
}