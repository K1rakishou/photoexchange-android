package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.interactors.FindPhotoAnswersUseCase
import com.kirakishou.photoexchange.mvp.model.FindPhotosData
import com.kirakishou.photoexchange.mvp.model.PhotoState
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.lang.ref.WeakReference

class FindPhotoAnswerServicePresenter(
    private val callbacks: WeakReference<FindPhotoAnswerServiceCallbacks>,
    private val myPhotosRepository: PhotosRepository,
    private val settingsRepository: SettingsRepository,
    private val schedulerProvider: SchedulerProvider,
    private val findPhotoAnswersUseCase: FindPhotoAnswersUseCase
) {

    private val TAG = "FindPhotoAnswerServicePresenter"
    private val findPhotosSubject = PublishSubject.create<Unit>().toSerialized()
    private var compositeDisposable = CompositeDisposable()

    init {
        compositeDisposable += findPhotosSubject
            .subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
            .map { myPhotosRepository.findAllByState(PhotoState.PHOTO_UPLOADED) }
            .filter { uploadedPhotos -> uploadedPhotos.isNotEmpty() }
            .map { uploadedPhotos ->
                val photoNames = uploadedPhotos
                    .joinToString(",") { it.photoName!! }

                val userId = settingsRepository.getUserId()

                Timber.tag(TAG).d("userId = $userId, photoNames = $photoNames")
                return@map FindPhotosData(userId, photoNames)
            }
            .doOnNext { photosData ->
                Timber.tag(TAG).d("Check if data is empty")
                if (photosData.isEmpty()) {
                    callbacks.get()?.stopService()
                }
            }
            .filter { photosData -> !photosData.isEmpty() }
            .doOnError { error ->
                callbacks.get()?.onError(error )
                callbacks.get()?.stopService()
            }
            .concatMap { data ->
                Timber.tag(TAG).d("getPhotoAnswers")
                findPhotoAnswersUseCase.getPhotoAnswers(data, callbacks).toObservable()
            }
            .doOnError { error ->
                Timber.tag(TAG).d("onError")
                callbacks.get()?.onError(error)
            }
            .doOnEach {
                Timber.tag(TAG).d("stopService")
                callbacks.get()?.stopService()
            }
            .subscribe()
    }

    fun onDetach() {
        this.compositeDisposable.clear()
    }

    fun startFindPhotoAnswers() {
        Timber.tag(TAG).d("startFindPhotoAnswers called")
        findPhotosSubject.onNext(Unit)
    }
}