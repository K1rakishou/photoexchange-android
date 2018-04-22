package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.interactors.FindPhotoAnswersUseCase
import com.kirakishou.photoexchange.mvp.model.FindPhotosData
import com.kirakishou.photoexchange.mvp.model.PhotoState
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import java.lang.ref.WeakReference

class FindPhotoAnswerServicePresenter(
    private val callbacks: WeakReference<FindPhotoAnswerServiceCallbacks>,
    private val myPhotosRepository: PhotosRepository,
    private val settingsRepository: SettingsRepository,
    private val schedulerProvider: SchedulerProvider,
    private val findPhotoAnswersUseCase: FindPhotoAnswersUseCase
) {

    private val findPhotosSubject = PublishSubject.create<FindPhotosData>().toSerialized()
    private var compositeDisposable = CompositeDisposable()

    init {
        compositeDisposable += findPhotosSubject
            .subscribeOn(schedulerProvider.BG())
            .observeOn(schedulerProvider.BG())
            .flatMap { data -> findPhotoAnswersUseCase.getPhotoAnswers(data, callbacks) }
            .doOnError { error -> callbacks.get()?.onError(error) }
            .doOnEach { callbacks.get()?.stopService() }
            .subscribe()
    }

    fun onDetach() {
        this.compositeDisposable.clear()
    }

    fun startFindPhotoAnswers() {
        compositeDisposable += Single.fromCallable { myPhotosRepository.findAllByState(PhotoState.PHOTO_UPLOADED) }
            .subscribeOn(schedulerProvider.BG())
            .observeOn(schedulerProvider.BG())
            .filter { uploadedPhotos -> uploadedPhotos.isNotEmpty() }
            .map { uploadedPhotos ->
                val photoNames = uploadedPhotos
                    .joinToString(",") { it.photoName!! }

                val userId = settingsRepository.getUserId()
                return@map FindPhotosData(userId, photoNames)
            }
            .doOnSuccess { photosData ->
                if (photosData.isEmpty()) {
                    callbacks.get()?.stopService()
                }
            }
            .filter { photosData -> !photosData.isEmpty() }
            .doOnError { error ->
                callbacks.get()?.onError(error )
                callbacks.get()?.stopService()
            }
            .subscribe(findPhotosSubject::onNext)
    }
}