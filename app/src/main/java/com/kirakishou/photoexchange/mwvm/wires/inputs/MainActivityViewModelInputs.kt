package com.kirakishou.photoexchange.mwvm.wires.inputs

import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto

/**
 * Created by kirakishou on 11/3/2017.
 */
interface MainActivityViewModelInputs {
    fun saveTakenPhoto(takenPhoto: TakenPhoto)
}