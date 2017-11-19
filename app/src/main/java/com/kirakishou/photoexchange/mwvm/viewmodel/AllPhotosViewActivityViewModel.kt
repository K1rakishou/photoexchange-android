package com.kirakishou.photoexchange.mwvm.viewmodel

import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.database.repository.PhotoAnswerRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mwvm.model.other.Pageable
import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer
import com.kirakishou.photoexchange.mwvm.model.other.UploadedPhoto
import com.kirakishou.photoexchange.mwvm.wires.errors.AllPhotosViewActivityViewModelErrors
import com.kirakishou.photoexchange.mwvm.wires.inputs.AllPhotosViewActivityViewModelInputs
import com.kirakishou.photoexchange.mwvm.wires.outputs.AllPhotosViewActivityViewModelOutputs
import io.reactivex.Observable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

/**
 * Created by kirakishou on 11/7/2017.
 */
class AllPhotosViewActivityViewModel(
        private val uploadedPhotosRepository: UploadedPhotosRepository,
        private val photoAnswerRepository: PhotoAnswerRepository,
        private val schedulers: SchedulerProvider
) : BaseViewModel(),
        AllPhotosViewActivityViewModelInputs,
        AllPhotosViewActivityViewModelOutputs,
        AllPhotosViewActivityViewModelErrors {

    val inputs: AllPhotosViewActivityViewModelInputs = this
    val outputs: AllPhotosViewActivityViewModelOutputs = this
    val errors: AllPhotosViewActivityViewModelErrors = this

    //inputs
    private val fetchOnePageUploadedPhotosSubject = PublishSubject.create<Pageable>()
    private val fetchOnePageReceivedPhotosSubject = PublishSubject.create<Pageable>()

    //outputs
    private val onUploadedPhotosPageReceivedSubject = PublishSubject.create<List<UploadedPhoto>>()
    private val onReceivedPhotosPageReceivedSubject = PublishSubject.create<List<PhotoAnswer>>()

    //errors

    init {
        compositeDisposable += fetchOnePageUploadedPhotosSubject
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .flatMap(uploadedPhotosRepository::findOnePage)
                .subscribe(onUploadedPhotosPageReceivedSubject::onNext, this::handleError)

        compositeDisposable += fetchOnePageReceivedPhotosSubject
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .flatMap(photoAnswerRepository::findOnePage)
                .subscribe(onReceivedPhotosPageReceivedSubject::onNext, this::handleError)
    }

    override fun fetchOnePageUploadedPhotos(page: Int, count: Int) {
        fetchOnePageUploadedPhotosSubject.onNext(Pageable(page, count))
    }

    override fun fetchOnePageReceivedPhotos(page: Int, count: Int) {
        fetchOnePageReceivedPhotosSubject.onNext(Pageable(page, count))
    }

    private fun handleError(error: Throwable) {
        Timber.e(error)
    }

    override fun onCleared() {
        Timber.d("AllPhotosViewActivityViewModel.onCleared()")

        super.onCleared()
    }

    override fun onUploadedPhotosPageReceivedObservable(): Observable<List<UploadedPhoto>> = onUploadedPhotosPageReceivedSubject
    override fun onReceivedPhotosPageReceivedObservable(): Observable<List<PhotoAnswer>> = onReceivedPhotosPageReceivedSubject
}
















