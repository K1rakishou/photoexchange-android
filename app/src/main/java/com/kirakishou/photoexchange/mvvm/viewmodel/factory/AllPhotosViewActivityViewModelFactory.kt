package com.kirakishou.photoexchange.mvvm.viewmodel.factory

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.kirakishou.photoexchange.mvvm.viewmodel.AllPhotosViewActivityViewModel
import javax.inject.Inject

/**
 * Created by kirakishou on 11/7/2017.
 */
class AllPhotosViewActivityViewModelFactory
@Inject constructor() : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return AllPhotosViewActivityViewModel() as T
    }
}