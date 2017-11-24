package com.kirakishou.photoexchange.mwvm.viewmodel

import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.util.FileUtils
import com.kirakishou.photoexchange.mwvm.model.other.Constants
import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto
import com.kirakishou.photoexchange.mwvm.wires.errors.MainActivityViewModelErrors
import com.kirakishou.photoexchange.mwvm.wires.inputs.MainActivityViewModelInputs
import com.kirakishou.photoexchange.mwvm.wires.outputs.MainActivityViewModelOutputs
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.rx2.await
import timber.log.Timber

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

    private val onTakenPhotoSavedOutput = PublishSubject.create<TakenPhoto>()
    private val onUnknownErrorErrorSubject = PublishSubject.create<Throwable>()

    override fun cleanTakenPhotosDB() {
        compositeJob += async {
            try {
                val takenPhotos = takenPhotosRepo.findAll().await()
                FileUtils.deletePhotosFiles(takenPhotos)

                takenPhotosRepo.deleteAll().await()
                printTakenPhotoTable()
            } catch (error: Throwable) {
                handleErrors(error)
            }
        }
    }

    override fun saveTakenPhoto(takenPhoto: TakenPhoto) {
        compositeJob += async {
            try {
                val id = takenPhotosRepo.saveOne(takenPhoto.location.lon, takenPhoto.location.lat,
                        takenPhoto.photoFilePath, takenPhoto.userId).await()
                val savedTakenPhoto = takenPhoto.copy(id)

                printTakenPhotoTable()
                onTakenPhotoSavedOutput.onNext(savedTakenPhoto)
            } catch (error: Throwable) {
                onTakenPhotoSavedOutput.onError(error)
            }
        }
    }

    private suspend fun printTakenPhotoTable() {
        if (Constants.isDebugBuild) {
            val allPhotos = takenPhotosRepo.findAll().await()
            allPhotos.forEach { Timber.d("photo: $it") }
        }
    }

    private fun handleErrors(error: Throwable) {
        Timber.e(error)
        onUnknownErrorErrorSubject.onNext(error)
    }

    override fun onCleared() {
        Timber.d("TakePhotoActivityViewModel.onCleared()")

        super.onCleared()
    }

    override fun onTakenPhotoSavedObservable(): Observable<TakenPhoto> = onTakenPhotoSavedOutput
    override fun onUnknownErrorObservable(): Observable<Throwable> = onUnknownErrorErrorSubject
}