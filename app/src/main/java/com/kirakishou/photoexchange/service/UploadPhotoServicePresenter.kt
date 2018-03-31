package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
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

    private val DELAY_BEFORE_UPLOADING_SECONDS = 10L
    private val uploadPhotosSubject = PublishSubject.create<UploadPhotoData>().toSerialized()

    private var compositeDisposable = CompositeDisposable()

    init {
        compositeDisposable += uploadPhotosSubject
            .subscribeOn(schedulerProvider.BG())
            .observeOn(schedulerProvider.BG())
            .throttleLast(DELAY_BEFORE_UPLOADING_SECONDS, TimeUnit.SECONDS)
            .doOnNext { data -> updatePhotosUseCase.uploadPhotos(data.userId, data.location, callbacks) }
            .doOnNext { callbacks.get()?.stopService() }
            .subscribe()
    }

    fun onDetach() {
        this.compositeDisposable.clear()
    }

    fun uploadPhotos() {
        val userId = settingsRepository.findUserId() ?: return
        val location = settingsRepository.findLastLocation() ?: return

        uploadPhotosSubject.onNext(UploadPhotoData(userId, location))
    }

    fun cancelPhotoUploading(photoId: Long) {
        myPhotosRepository.deleteByIdAndState(photoId, PhotoState.PHOTO_QUEUED_UP)
    }

    fun cancelAllPhotosUploading() {
        myPhotosRepository.deleteAllWithState(PhotoState.PHOTO_QUEUED_UP)
    }

    class UploadPhotoData(
        val userId: String,
        val location: LonLat
    )
}