package com.kirakishou.photoexchange.mvvm.viewmodel

import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvvm.model.Pageable
import com.kirakishou.photoexchange.mvvm.model.TakenPhoto
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.error.AllPhotosViewActivityViewModelError
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.input.AllPhotosViewActivityViewModelInputs
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.output.AllPhotosViewActivityViewModelOutputs
import io.reactivex.Observable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by kirakishou on 11/7/2017.
 */
class AllPhotosViewActivityViewModel
@Inject constructor(
        val takenPhotosRepository: TakenPhotosRepository,
        val schedulers: SchedulerProvider
) : BaseViewModel(),
        AllPhotosViewActivityViewModelInputs,
        AllPhotosViewActivityViewModelOutputs,
        AllPhotosViewActivityViewModelError {

    val inputs: AllPhotosViewActivityViewModelInputs = this
    val outputs: AllPhotosViewActivityViewModelOutputs = this
    val errors: AllPhotosViewActivityViewModelError = this

    private val onFailedToUploadPhotosSubject = PublishSubject.create<List<TakenPhoto>>()
    private val onLastTakenPhotoSubject = PublishSubject.create<TakenPhoto>()
    private val getLastTakenPhotoSubject = PublishSubject.create<Unit>()
    private val onTakenPhotosPageFetchedSubject = PublishSubject.create<List<TakenPhoto>>()
    private val getTakenPhotosPageSubject = PublishSubject.create<Pageable>()

    init {
        compositeDisposable += getTakenPhotosPageSubject
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .flatMap { takenPhotosRepository.findOnePage(it).toObservable() }
                .subscribe(onTakenPhotosPageFetchedSubject::onNext, this::handleError)

        compositeDisposable += getLastTakenPhotoSubject
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .flatMap { takenPhotosRepository.findLastSaved().toObservable() }
                .doOnNext(onLastTakenPhotoSubject::onNext)
                .flatMap { takenPhotosRepository.findFailedToUploadPhotos().toObservable() }
                .doOnNext(onFailedToUploadPhotosSubject::onNext)
                .doOnNext {
                    println(it.toString())
                    println()
                }
                .doOnError(this::handleError)
                .subscribe()
    }

    override fun getTakenPhotos(page: Int, count: Int) {
        getTakenPhotosPageSubject.onNext(Pageable(page, count))
    }

    override fun getLastTakenPhoto() {
        getLastTakenPhotoSubject.onNext(Unit)
    }

    private fun handleError(error: Throwable) {
        Timber.e(error)
    }

    override fun onCleared() {
        Timber.e("AllPhotosViewActivityViewModel.onCleared()")

        super.onCleared()
    }

    override fun onFailedToUploadPhotosObservable(): Observable<List<TakenPhoto>> = onFailedToUploadPhotosSubject
    override fun onLastTakenPhotoObservable(): Observable<TakenPhoto> = onLastTakenPhotoSubject
    override fun onTakenPhotosPageFetchedObservable(): Observable<List<TakenPhoto>> = onTakenPhotosPageFetchedSubject
}