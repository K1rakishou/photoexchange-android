package com.kirakishou.photoexchange.mvvm.viewmodel

import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvvm.model.ErrorCode
import com.kirakishou.photoexchange.mvvm.model.LonLat
import com.kirakishou.photoexchange.mvvm.model.dto.PhotoWithInfo
import com.kirakishou.photoexchange.mvvm.model.net.response.SendPhotoResponse
import com.kirakishou.photoexchange.mvvm.model.net.response.StatusResponse
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.error.MainActivityViewModelErrors
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.input.MainActivityViewModelInputs
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.output.MainActivityViewModelOutputs
import io.reactivex.Observable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.io.File
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
        Timber.e("ClientMainActivityViewModel.onCleared()")

        super.onCleared()
    }
}