package com.kirakishou.photoexchange.mvvm.viewmodel

import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.error.MainActivityViewModelErrors
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.input.MainActivityViewModelInputs
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.output.MainActivityViewModelOutputs
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by kirakishou on 11/3/2017.
 */
class MainActivityViewModel
@Inject constructor() : BaseViewModel(),
        MainActivityViewModelInputs,
        MainActivityViewModelOutputs,
        MainActivityViewModelErrors {

    val inputs: MainActivityViewModelInputs = this
    val outputs: MainActivityViewModelOutputs = this
    val errors: MainActivityViewModelErrors = this


    override fun onCleared() {
        Timber.e("MainActivityViewModel.onCleared()")

        super.onCleared()
    }
}