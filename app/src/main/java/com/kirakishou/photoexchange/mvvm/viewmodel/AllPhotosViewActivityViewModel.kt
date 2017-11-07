package com.kirakishou.photoexchange.mvvm.viewmodel

import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.mvvm.model.LonLat
import timber.log.Timber

/**
 * Created by kirakishou on 11/7/2017.
 */
class AllPhotosViewActivityViewModel : BaseViewModel() {

    var photoInfo: PhotoInfo? = null

    override fun onCleared() {
        Timber.e("AllPhotosViewActivityViewModel.onCleared()")

        super.onCleared()
    }

    data class PhotoInfo(val location: LonLat,
                         val userId: String,
                         val photoFilePath: String)
}