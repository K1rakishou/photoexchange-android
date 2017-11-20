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
import java.util.concurrent.TimeUnit

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
    private val scrollToTopInput = PublishSubject.create<Unit>()
    private val showLookingForPhotoIndicatorInput = PublishSubject.create<Unit>()
    private val showPhotoUploadedInput = PublishSubject.create<UploadedPhoto>()
    private val showFailedToUploadPhotoInput = PublishSubject.create<Unit>()
    private val showPhotoReceivedInput = PublishSubject.create<PhotoAnswer>()
    private val showErrorWhileTryingToLookForPhotoInput = PublishSubject.create<Unit>()
    private val showNoPhotoOnServerInput = PublishSubject.create<Unit>()
    private val showUserNeedsToUploadMorePhotosInput = PublishSubject.create<Unit>()

    //outputs
    private val onUploadedPhotosPageReceivedSubject = PublishSubject.create<List<UploadedPhoto>>()
    private val onReceivedPhotosPageReceivedSubject = PublishSubject.create<List<PhotoAnswer>>()
    private val scrollToTopOutput = PublishSubject.create<Unit>()
    private val showLookingForPhotoIndicatorOutput = PublishSubject.create<Unit>()
    private val showPhotoUploadedOutput = PublishSubject.create<UploadedPhoto>()
    private val showFailedToUploadPhotoOutput = PublishSubject.create<Unit>()
    private val showPhotoReceivedOutput = PublishSubject.create<PhotoAnswer>()
    private val showErrorWhileTryingToLookForPhotoOutput = PublishSubject.create<Unit>()
    private val showNoPhotoOnServerOutput = PublishSubject.create<Unit>()
    private val showUserNeedsToUploadMorePhotosOutput = PublishSubject.create<Unit>()

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

        compositeDisposable += scrollToTopInput
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .delay(500, TimeUnit.MILLISECONDS)
                .subscribe(scrollToTopOutput::onNext, this::handleError)

        compositeDisposable += showLookingForPhotoIndicatorInput
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .subscribe(showLookingForPhotoIndicatorOutput::onNext, this::handleError)

        compositeDisposable += showPhotoUploadedInput
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .subscribe(showPhotoUploadedOutput::onNext, this::handleError)

        compositeDisposable += showFailedToUploadPhotoInput
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .subscribe(showFailedToUploadPhotoOutput::onNext, this::handleError)

        compositeDisposable += showPhotoReceivedInput
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .subscribe(showPhotoReceivedOutput::onNext, this::handleError)

        compositeDisposable += showErrorWhileTryingToLookForPhotoInput
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .subscribe(showErrorWhileTryingToLookForPhotoOutput::onNext, this::handleError)

        compositeDisposable += showNoPhotoOnServerInput
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .subscribe(showNoPhotoOnServerOutput::onNext, this::handleError)

        compositeDisposable += showUserNeedsToUploadMorePhotosInput
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .subscribe(showUserNeedsToUploadMorePhotosOutput::onNext, this::handleError)
    }

    override fun fetchOnePageUploadedPhotos(page: Int, count: Int) {
        fetchOnePageUploadedPhotosSubject.onNext(Pageable(page, count))
    }

    override fun fetchOnePageReceivedPhotos(page: Int, count: Int) {
        fetchOnePageReceivedPhotosSubject.onNext(Pageable(page, count))
    }

    override fun receivedPhotosFragmentScrollToTop() {
        scrollToTopInput.onNext(Unit)
    }

    override fun receivedPhotosFragmentShowLookingForPhotoIndicator() {
        showLookingForPhotoIndicatorInput.onNext(Unit)
    }

    override fun uploadedPhotosFragmentShowPhotoUploaded(photo: UploadedPhoto) {
        showPhotoUploadedInput.onNext(photo)
    }

    override fun uploadedPhotosFragmentShowFailedToUploadPhoto() {
        showFailedToUploadPhotoInput.onNext(Unit)
    }

    override fun receivedPhotosFragmentShowPhotoReceived(photo: PhotoAnswer) {
        showPhotoReceivedInput.onNext(photo)
    }

    override fun receivedPhotosFragmentShowErrorWhileTryingToLookForPhoto() {
        showErrorWhileTryingToLookForPhotoInput.onNext(Unit)
    }

    override fun receivedPhotosFragmentShowNoPhotoOnServer() {
        showNoPhotoOnServerInput.onNext(Unit)
    }

    override fun receivedPhotosFragmentShowUserNeedsToUploadMorePhotos() {
        showUserNeedsToUploadMorePhotosInput.onNext(Unit)
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
    override fun onScrollToTopObservable(): Observable<Unit> = scrollToTopOutput
    override fun onShowLookingForPhotoIndicatorObservable(): Observable<Unit> = showLookingForPhotoIndicatorOutput
    override fun onShowPhotoUploadedOutputObservable(): Observable<UploadedPhoto> = showPhotoUploadedOutput
    override fun onShowFailedToUploadPhotoObservable(): Observable<Unit> = showFailedToUploadPhotoOutput
    override fun onShowPhotoReceivedObservable(): Observable<PhotoAnswer> = showPhotoReceivedOutput
    override fun onShowErrorWhileTryingToLookForPhotoObservable(): Observable<Unit> = showErrorWhileTryingToLookForPhotoOutput
    override fun onShowNoPhotoOnServerObservable(): Observable<Unit> = showNoPhotoOnServerOutput
    override fun onShowUserNeedsToUploadMorePhotosObservable(): Observable<Unit> = showUserNeedsToUploadMorePhotosOutput

}
















