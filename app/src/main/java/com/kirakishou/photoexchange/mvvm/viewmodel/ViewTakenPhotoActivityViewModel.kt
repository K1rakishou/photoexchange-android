package com.kirakishou.photoexchange.mvvm.viewmodel

import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvvm.model.TakenPhoto
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.error.ViewTakenPhotoActivityViewModelErrors
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.input.ViewTakenPhotoActivityViewModelInputs
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.output.ViewTakenPhotoActivityViewModelOutputs
import io.reactivex.Observable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Created by kirakishou on 11/9/2017.
 */
class ViewTakenPhotoActivityViewModel
@Inject constructor(
        private val takenPhotosRepo: TakenPhotosRepository,
        private val schedulers: SchedulerProvider
) : BaseViewModel(),
        ViewTakenPhotoActivityViewModelInputs,
        ViewTakenPhotoActivityViewModelOutputs,
        ViewTakenPhotoActivityViewModelErrors {

    val inputs: ViewTakenPhotoActivityViewModelInputs = this
    val outputs: ViewTakenPhotoActivityViewModelOutputs = this
    val errors: ViewTakenPhotoActivityViewModelErrors = this

    private val onPhotoDeletedSubject = PublishSubject.create<Unit>()
    private val deletePhotoSubject = PublishSubject.create<Long>()

    init {
        compositeDisposable += deletePhotoSubject
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .flatMap { photoId -> takenPhotosRepo.findOne(photoId).toObservable() }
                .doOnNext(this::deleteFileFromDisk)
                .flatMap { takenPhoto -> takenPhotosRepo.deleteOne(takenPhoto.id).toObservable() }
                .map { Unit }
                .subscribe(onPhotoDeletedSubject::onNext, this::handleError)
    }

    override fun deletePhoto(photoId: Long) {
        deletePhotoSubject.onNext(photoId)
    }

    private fun handleError(error: Throwable) {
        Timber.e(error)
    }

    override fun onCleared() {
        Timber.e("ViewTakenPhotoActivityViewModel.onCleared()")

        super.onCleared()
    }

    private fun deleteFileFromDisk(takenPhoto: TakenPhoto) {
        if (takenPhoto.isEmpty()) {
            Timber.e("Could not find photo by id")
            return
        }

        val photoFile = File(takenPhoto.photoFilePath)
        if (photoFile.exists()) {
            photoFile.delete()
        }
    }

    override fun onPhotoDeletedObservable(): Observable<Unit> = onPhotoDeletedSubject
}