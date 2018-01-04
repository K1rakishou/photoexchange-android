package com.kirakishou.photoexchange.mwvm.wires.inputs

/**
 * Created by kirakishou on 11/9/2017.
 */
interface ViewTakenPhotoActivityViewModelInputs {
    fun deleteTakenPhoto(id: Long)
    fun updateTakenPhotoAsQueuedUp(id: Long)
}