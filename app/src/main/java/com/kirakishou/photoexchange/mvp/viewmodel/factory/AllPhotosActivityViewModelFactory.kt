package com.kirakishou.photoexchange.mvp.viewmodel.factory

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.interactors.FavouritePhotoUseCase
import com.kirakishou.photoexchange.interactors.GetGalleryPhotosUseCase
import com.kirakishou.photoexchange.interactors.ReportPhotoUseCase
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import javax.inject.Inject

/**
 * Created by kirakishou on 3/11/2018.
 */
class AllPhotosActivityViewModelFactory
@Inject constructor(
    val photosRepository: PhotosRepository,
    val settingsRepository: SettingsRepository,
    val receivedPhotosRepository: ReceivedPhotosRepository,
    val galleryPhotosUseCase: GetGalleryPhotosUseCase,
    val favouritePhotoUseCase: FavouritePhotoUseCase,
    val reportPhotoUseCase: ReportPhotoUseCase,
    val schedulerProvider: SchedulerProvider
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return PhotosActivityViewModel(
            photosRepository,
            settingsRepository,
            receivedPhotosRepository,
            galleryPhotosUseCase,
            favouritePhotoUseCase,
            reportPhotoUseCase,
            schedulerProvider
        ) as T
    }
}