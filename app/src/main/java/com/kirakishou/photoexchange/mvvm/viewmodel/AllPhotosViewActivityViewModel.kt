package com.kirakishou.photoexchange.mvvm.viewmodel

import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.mvvm.model.LonLat
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.error.AllPhotosViewActivityViewModelError
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.input.AllPhotosViewActivityViewModelInputs
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.output.AllPhotosViewActivityViewModelOutputs
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by kirakishou on 11/7/2017.
 */
class AllPhotosViewActivityViewModel
@Inject constructor(
        val takenPhotosRepository: TakenPhotosRepository
) : BaseViewModel(),
        AllPhotosViewActivityViewModelInputs,
        AllPhotosViewActivityViewModelOutputs,
        AllPhotosViewActivityViewModelError {

    val inputs: AllPhotosViewActivityViewModelInputs = this
    val outputs: AllPhotosViewActivityViewModelOutputs = this
    val errors: AllPhotosViewActivityViewModelError = this

    override fun onCleared() {
        Timber.e("AllPhotosViewActivityViewModel.onCleared()")

        super.onCleared()
    }
}