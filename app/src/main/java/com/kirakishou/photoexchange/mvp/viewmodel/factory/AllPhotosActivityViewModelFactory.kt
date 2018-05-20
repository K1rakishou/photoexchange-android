package com.kirakishou.photoexchange.mvp.viewmodel.factory

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.interactors.*
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import javax.inject.Inject

/**
 * Created by kirakishou on 3/11/2018.
 */
class AllPhotosActivityViewModelFactory
@Inject constructor(
    val takenPhotosRepository: TakenPhotosRepository,
    val uploadedPhotosRepository: UploadedPhotosRepository,
    val settingsRepository: SettingsRepository,
    val receivedPhotosRepository: ReceivedPhotosRepository,
    val galleryPhotosUseCase: GetGalleryPhotosUseCase,
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
            settingsRepository,
            receivedPhotosRepository,
            galleryPhotosUseCase,
            getUploadedPhotosUseCase,
            getReceivedPhotosUseCase,
            favouritePhotoUseCase,
            reportPhotoUseCase,
            schedulerProvider
        ) as T
    }
}