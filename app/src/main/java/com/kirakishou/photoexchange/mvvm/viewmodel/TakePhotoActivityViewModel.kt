package com.kirakishou.photoexchange.mvvm.viewmodel

import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvvm.model.Constants
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.error.MainActivityViewModelErrors
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.input.MainActivityViewModelInputs
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.output.MainActivityViewModelOutputs
import io.reactivex.rxkotlin.plusAssign
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by kirakishou on 11/3/2017.
 */
class TakePhotoActivityViewModel
@Inject constructor(
        private val uploadedPhotosRepo: UploadedPhotosRepository,
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

    /*//TODO: redo this asynchronously
    fun saveTakenPhotoToTheDb(location: LonLat, userId: String, photoFilePath: String): Long {
        val id = uploadedPhotosRepo.saveOne(location.lon, location.lat, userId, photoFilePath)

        if (Constants.isDebugBuild) {
            val allPhotos = uploadedPhotosRepo.findAll()
            allPhotos.forEach { Timber.d("photo: $it") }
        }

        return id
    }*/

    override fun cleanDb() {
        compositeDisposable += uploadedPhotosRepo.deleteAllNotSent()
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .doOnNext {
                    if (Constants.isDebugBuild) {
                        val allPhotos = uploadedPhotosRepo.findAll().blockingFirst()
                        allPhotos.forEach { Timber.d("photo: $it") }
                    }
                }
                .doOnError(this::handlerError)
                .subscribe()
    }

    private fun handlerError(error: Throwable) {
        Timber.e(error)
    }

    override fun onCleared() {
        Timber.e("TakePhotoActivityViewModel.onCleared()")

        super.onCleared()
    }
}