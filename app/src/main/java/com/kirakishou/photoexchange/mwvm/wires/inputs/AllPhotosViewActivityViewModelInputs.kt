package com.kirakishou.photoexchange.mwvm.wires.inputs

/**
 * Created by kirakishou on 11/8/2017.
 */
interface AllPhotosViewActivityViewModelInputs {
    fun fetchOnePageUploadedPhotos(page: Int, count: Int)
    fun fetchOnePageReceivedPhotos(page: Int, count: Int)
}