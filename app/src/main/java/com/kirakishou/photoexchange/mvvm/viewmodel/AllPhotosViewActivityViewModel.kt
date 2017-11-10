package com.kirakishou.photoexchange.mvvm.viewmodel

import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.error.AllPhotosViewActivityViewModelErrors
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.input.AllPhotosViewActivityViewModelInputs
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.output.AllPhotosViewActivityViewModelOutputs
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by kirakishou on 11/7/2017.
 */
class AllPhotosViewActivityViewModel
@Inject constructor(
        private val uploadedPhotosRepository: UploadedPhotosRepository,
        private val schedulers: SchedulerProvider
) : BaseViewModel(),
        AllPhotosViewActivityViewModelInputs,
        AllPhotosViewActivityViewModelOutputs,
        AllPhotosViewActivityViewModelErrors {

    val inputs: AllPhotosViewActivityViewModelInputs = this
    val outputs: AllPhotosViewActivityViewModelOutputs = this
    val errors: AllPhotosViewActivityViewModelErrors = this

    init {

    }

    private fun handleError(error: Throwable) {
        Timber.e(error)
    }

    override fun onCleared() {
        Timber.e("AllPhotosViewActivityViewModel.onCleared()")

        super.onCleared()
    }
}