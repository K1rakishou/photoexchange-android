package com.kirakishou.photoexchange.mvvm.viewmodel

import com.kirakishou.photoexchange.base.BaseViewModel
import timber.log.Timber

/**
 * Created by kirakishou on 11/7/2017.
 */
class AllPhotosViewActivityViewModel : BaseViewModel() {

    override fun onCleared() {
        Timber.e("AllPhotosViewActivityViewModel.onCleared()")

        super.onCleared()
    }
}