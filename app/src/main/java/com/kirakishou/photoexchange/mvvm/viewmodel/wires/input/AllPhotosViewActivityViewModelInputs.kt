package com.kirakishou.photoexchange.mvvm.viewmodel.wires.input

/**
 * Created by kirakishou on 11/8/2017.
 */
interface AllPhotosViewActivityViewModelInputs {
    fun getTakenPhotos(page: Int, count: Int)
    fun getLastTakenPhoto()
}