package com.kirakishou.photoexchange.mvvm.viewmodel

import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvvm.model.other.Constants
import com.kirakishou.photoexchange.mvvm.model.other.TakenPhoto
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.error.MainActivityViewModelErrors
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.input.MainActivityViewModelInputs
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.output.MainActivityViewModelOutputs
import io.reactivex.rxkotlin.plusAssign
import timber.log.Timber
import java.io.File

/**
 * Created by kirakishou on 11/3/2017.
 */
class TakePhotoActivityViewModel(
        private val uploadedPhotosRepo: UploadedPhotosRepository,
        private val takenPhotosRepo: TakenPhotosRepository,
        private val schedulers: SchedulerProvider
) : BaseViewModel(),
        MainActivityViewModelInputs,
        MainActivityViewModelOutputs,
        MainActivityViewModelErrors {

    val inputs: MainActivityViewModelInputs = this
    val outputs: MainActivityViewModelOutputs = this
    val errors: MainActivityViewModelErrors = this

    init {

    }

    override fun cleanDb() {
        compositeDisposable += takenPhotosRepo.findAll()
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .doOnSuccess(this::deletePhotoFiles)
                .flatMap { takenPhotosRepo.deleteAll() }
                .doOnSuccess {
                    if (Constants.isDebugBuild) {
                        val allPhotos = takenPhotosRepo.findAll().blockingGet()
                        allPhotos.forEach { Timber.d("photo: $it") }
                    }
                }
                .doOnError(this::handlerError)
                .subscribe()
    }

    private fun deletePhotoFiles(allPhotos: List<TakenPhoto>) {
        allPhotos.forEach { uploadedPhoto ->
            val photoFile = File(uploadedPhoto.photoFilePath)
            if (photoFile.exists()) {
                val wasDeleted = photoFile.delete()
                if (!wasDeleted) {
                    Timber.e("Could not delete file: ${uploadedPhoto.photoFilePath}")
                }
            }
        }
    }

    private fun handlerError(error: Throwable) {
        Timber.e(error)
    }

    override fun onCleared() {
        Timber.e("TakePhotoActivityViewModel.onCleared()")

        super.onCleared()
    }
}