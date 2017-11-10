package com.kirakishou.photoexchange.mvvm.viewmodel

import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.error.ViewTakenPhotoActivityViewModelErrors
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.input.ViewTakenPhotoActivityViewModelInputs
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.output.ViewTakenPhotoActivityViewModelOutputs
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by kirakishou on 11/9/2017.
 */
class ViewTakenPhotoActivityViewModel
@Inject constructor(
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

    private fun handleError(error: Throwable) {
        Timber.e(error)
    }

    override fun onCleared() {
        Timber.e("ViewTakenPhotoActivityViewModel.onCleared()")

        super.onCleared()
    }
}