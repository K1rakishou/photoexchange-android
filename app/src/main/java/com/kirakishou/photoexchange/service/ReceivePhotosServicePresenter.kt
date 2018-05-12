package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.interactors.ReceivePhotosUseCase
import com.kirakishou.photoexchange.mvp.model.FindPhotosData
import com.kirakishou.photoexchange.mvp.model.PhotoState
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.lang.ref.WeakReference

class ReceivePhotosServicePresenter(
    private val callbacks: WeakReference<ReceivePhotosServiceCallbacks>,
    private val takenPhotosRepository: TakenPhotosRepository,
    private val uploadedPhotosRepository: UploadedPhotosRepository,
    private val settingsRepository: SettingsRepository,
    private val schedulerProvider: SchedulerProvider,
    private val receivePhotosUseCase: ReceivePhotosUseCase
) {

    private val TAG = "ReceivePhotosServicePresenter"
    private val findPhotosSubject = PublishSubject.create<Unit>().toSerialized()
    private var compositeDisposable = CompositeDisposable()

    init {
        compositeDisposable += findPhotosSubject
            .subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
            .flatMap {
                return@flatMap Observable.just(1)
                    .subscribeOn(schedulerProvider.IO())
                    .flatMap {
                        Observable.fromCallable { uploadedPhotosRepository.findAll(false) }
                            .doOnNext { uploadedPhotos ->
                                if (uploadedPhotos.isEmpty()) {
                                    callbacks.get()?.stopService()
                                }
                            }
                    }
                    .filter { uploadedPhotos -> uploadedPhotos.isNotEmpty() }
                    .map { uploadedPhotos ->
                        val photoNames = uploadedPhotos
                            .joinToString(",") { it.photoName }

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
                        Timber.tag(TAG).d("receivePhotos")
                        receivePhotosUseCase.receivePhotos(data, callbacks).toObservable()
                    }
                    .doOnError { error ->
                        Timber.tag(TAG).d("onError")
                        callbacks.get()?.onError(error)
                    }
                    .doOnEach {
                        Timber.tag(TAG).d("stopService")
                        callbacks.get()?.stopService()
                    }
                    .map { Unit }
                    .onErrorReturnItem(Unit)
            }
            .subscribe()
    }

    fun onDetach() {
        this.compositeDisposable.clear()
    }

    fun startPhotosReceiving() {
        Timber.tag(TAG).d("startPhotosReceiving called")
        findPhotosSubject.onNext(Unit)
    }
}