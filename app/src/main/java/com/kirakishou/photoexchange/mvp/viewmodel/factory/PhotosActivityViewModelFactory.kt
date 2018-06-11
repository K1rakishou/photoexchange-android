package com.kirakishou.photoexchange.mvp.viewmodel.factory

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.*
import com.kirakishou.photoexchange.interactors.*
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import javax.inject.Inject

/**
 * Created by kirakishou on 3/11/2018.
 */
class PhotosActivityViewModelFactory
@Inject constructor(
    val takenPhotosRepository: TakenPhotosRepository,
    val uploadedPhotosRepository: UploadedPhotosRepository,
    val galleryPhotoRepository: GalleryPhotoRepository,
    val settingsRepository: SettingsRepository,
    val receivedPhotosRepository: ReceivedPhotosRepository,
    val galleryPhotosUseCase: GetGalleryPhotosUseCase,
    val getGalleryPhotosInfoUseCase: GetGalleryPhotosInfoUseCase,
    val favouritePhotoUseCase: FavouritePhotoUseCase,
    val reportPhotoUseCase: ReportPhotoUseCase,
    val getUploadedPhotosUseCase: GetUploadedPhotosUseCase,
    val getReceivedPhotosUseCase: GetReceivedPhotosUseCase,
    val schedulerProvider: SchedulerProvider
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return PhotosActivityViewModel(
            takenPhotosRepository,
            uploadedPhotosRepository,
            galleryPhotoRepository,
            settingsRepository,
            receivedPhotosRepository,
            galleryPhotosUseCase,
            getGalleryPhotosInfoUseCase,
            getUploadedPhotosUseCase,
            getReceivedPhotosUseCase,
            favouritePhotoUseCase,
            reportPhotoUseCase,
            schedulerProvider
        ) as T
    }
}