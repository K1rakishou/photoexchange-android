package com.kirakishou.photoexchange.mwvm.viewmodel

import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.util.FileUtils
import com.kirakishou.photoexchange.mwvm.wires.errors.ViewTakenPhotoActivityViewModelErrors
import com.kirakishou.photoexchange.mwvm.wires.inputs.ViewTakenPhotoActivityViewModelInputs
import com.kirakishou.photoexchange.mwvm.wires.outputs.ViewTakenPhotoActivityViewModelOutputs
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.rx2.await
import timber.log.Timber

/**
 * Created by kirakishou on 11/9/2017.
 */
class ViewTakenPhotoActivityViewModel(
        private val takenPhotosRepo: TakenPhotosRepository,
        private val uploadedPhotosRepo: UploadedPhotosRepository,
        private val schedulers: SchedulerProvider
) : BaseViewModel(),
        ViewTakenPhotoActivityViewModelInputs,
        ViewTakenPhotoActivityViewModelOutputs,
        ViewTakenPhotoActivityViewModelErrors {

    val inputs: ViewTakenPhotoActivityViewModelInputs = this
    val outputs: ViewTakenPhotoActivityViewModelOutputs = this
    val errors: ViewTakenPhotoActivityViewModelErrors = this

    init {

    }

    override fun deleteTakenPhoto(id: Long) {
        compositeJob += async {
            val takenPhoto = takenPhotosRepo.findOne(id).await()
            FileUtils.deletePhotoFile(takenPhoto)

            takenPhotosRepo.deleteOne(id).await()
        }
    }

    private fun handleErrors(error: Throwable) {
        Timber.e(error)
    }

    override fun onCleared() {
        Timber.d("ViewTakenPhotoActivityViewModel.onCleared()")

        super.onCleared()
    }
}