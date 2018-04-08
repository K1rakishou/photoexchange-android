package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.PhotoUploadingEvent
import com.kirakishou.photoexchange.mvp.model.UploadPhotoData
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
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
    private val myPhotosRepository: PhotosRepository,
    private val settingsRepository: SettingsRepository,
    private val schedulerProvider: SchedulerProvider,
    private val updatePhotosUseCase: UploadPhotosUseCase
) {
    private val tag = "[${this::class.java.simpleName}] "
    private val uploadPhotosSubject = PublishSubject.create<UploadPhotoData>().toSerialized()
    private var compositeDisposable = CompositeDisposable()

    init {
        compositeDisposable += uploadPhotosSubject
            .subscribeOn(schedulerProvider.BG())
            .observeOn(schedulerProvider.BG())
            .filter { !it.isEmpty() }
            .doOnEach {
                val queuedUpPhotosCount = myPhotosRepository.countAllByState(PhotoState.PHOTO_QUEUED_UP).toInt()
                callbacks.get()?.onUploadingEvent(PhotoUploadingEvent.OnPrepare(queuedUpPhotosCount))
            }
            .doOnNext { data ->
                updatePhotosUseCase.uploadPhotos(data.userId, data.location, callbacks)
                callbacks.get()?.onUploadingEvent(PhotoUploadingEvent.OnEnd())
            }
            .doOnError { callbacks.get()?.onUploadingEvent(PhotoUploadingEvent.OnUnknownError()) }
            .doOnEach { callbacks.get()?.stopService() }
            .subscribe()
    }

    fun onDetach() {
        this.compositeDisposable.clear()
    }

    fun uploadPhotos() {
        compositeDisposable += Single.fromCallable {
            val userId = settingsRepository.findUserId()
                ?: return@fromCallable UploadPhotoData.empty()
            val location = settingsRepository.findLastLocation()
                ?: return@fromCallable UploadPhotoData.empty()

            return@fromCallable UploadPhotoData(false, userId, location)

        }
        .subscribeOn(schedulerProvider.BG())
        .observeOn(schedulerProvider.BG())
        .subscribe(uploadPhotosSubject::onNext, uploadPhotosSubject::onError)
    }
}