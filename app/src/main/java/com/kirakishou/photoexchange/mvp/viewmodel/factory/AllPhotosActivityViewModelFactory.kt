package com.kirakishou.photoexchange.mvp.viewmodel.factory

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.mvp.view.AllPhotosActivityView
import com.kirakishou.photoexchange.mvp.viewmodel.AllPhotosActivityViewModel
import javax.inject.Inject

/**
 * Created by kirakishou on 3/11/2018.
 */
class AllPhotosActivityViewModelFactory
@Inject constructor(
    val view: AllPhotosActivityView,
    val photosRepository: PhotosRepository,
    val settingsRepository: SettingsRepository,
    val coroutinesPool: CoroutineThreadPoolProvider
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return AllPhotosActivityViewModel(view, photosRepository, settingsRepository, coroutinesPool) as T
    }
}