package com.kirakishou.photoexchange.mvp.viewmodel.factory

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.mvp.viewmodel.ViewTakenPhotoActivityViewModel
import javax.inject.Inject

/**
 * Created by kirakishou on 3/9/2018.
 */
class ViewTakenPhotoActivityViewModelFactory
@Inject constructor(
    val schedulerProvider: SchedulerProvider,
    val photosRepository: PhotosRepository,
    val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ViewTakenPhotoActivityViewModel(schedulerProvider, photosRepository, settingsRepository) as T
    }
}