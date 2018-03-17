package com.kirakishou.photoexchange.mvp.viewmodel.factory

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.mvp.view.ViewTakenPhotoActivityView
import com.kirakishou.photoexchange.mvp.viewmodel.ViewTakenPhotoActivityViewModel
import javax.inject.Inject

/**
 * Created by kirakishou on 3/9/2018.
 */
class ViewTakenPhotoActivityViewModelFactory
@Inject constructor(
    val view: ViewTakenPhotoActivityView,
    val coroutinePool: CoroutineThreadPoolProvider,
    val photosRepository: PhotosRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ViewTakenPhotoActivityViewModel(view, coroutinePool, photosRepository) as T
    }
}