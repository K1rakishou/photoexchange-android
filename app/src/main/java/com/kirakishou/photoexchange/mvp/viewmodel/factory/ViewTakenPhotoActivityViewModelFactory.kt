package com.kirakishou.photoexchange.mvp.viewmodel.factory

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.kirakishou.photoexchange.mvp.view.ViewTakenPhotoActivityView
import com.kirakishou.photoexchange.mvp.viewmodel.ViewTakenPhotoActivityViewModel
import javax.inject.Inject

/**
 * Created by kirakishou on 3/9/2018.
 */
class ViewTakenPhotoActivityViewModelFactory
@Inject constructor(
    val view: ViewTakenPhotoActivityView
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ViewTakenPhotoActivityViewModel(view) as T
    }
}