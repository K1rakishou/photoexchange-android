package com.kirakishou.photoexchange.di.module

import android.arch.lifecycle.ViewModelProviders
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.*
import com.kirakishou.photoexchange.interactors.*
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.factory.PhotosActivityViewModelFactory
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import dagger.Module
import dagger.Provides

/**
 * Created by kirakishou on 3/11/2018.
 */

@Module
open class PhotosActivityModule(
    val activity: PhotosActivity
) {

    @PerActivity
    @Provides
    fun provideViewModelFactory(imageLoader: ImageLoader,
                                schedulerProvider: SchedulerProvider,
                                takenPhotosRepository: TakenPhotosRepository,
                                uploadedPhotosRepository: UploadedPhotosRepository,
                                galleryPhotoRepository: GalleryPhotoRepository,
                                getGalleryPhotosInfoUseCase: GetGalleryPhotosInfoUseCase,
                                receivedPhotosRepository: ReceivedPhotosRepository,
                                settingsRepository: SettingsRepository,
                                galleryPhotosUseCase: GetGalleryPhotosUseCase,
                                favouritePhotoUseCase: FavouritePhotoUseCase,
                                getUploadedPhotosUseCase: GetUploadedPhotosUseCase,
                                getReceivedPhotosUseCase: GetReceivedPhotosUseCase,
                                reportPhotoUseCase: ReportPhotoUseCase): PhotosActivityViewModelFactory {
        return PhotosActivityViewModelFactory(
            imageLoader,
            takenPhotosRepository,
            uploadedPhotosRepository,
            galleryPhotoRepository,
            settingsRepository,
            receivedPhotosRepository,
            galleryPhotosUseCase,
            getGalleryPhotosInfoUseCase,
            favouritePhotoUseCase,
            reportPhotoUseCase,
            getUploadedPhotosUseCase,
            getReceivedPhotosUseCase,
            schedulerProvider,
            Constants.ADAPTER_LOAD_MORE_ITEMS_DELAY_MS,
            Constants.PROGRESS_FOOTER_REMOVE_DELAY_MS)
    }

    @PerActivity
    @Provides
    fun provideViewModel(viewModelFactory: PhotosActivityViewModelFactory): PhotosActivityViewModel {
        return ViewModelProviders.of(activity, viewModelFactory).get(PhotosActivityViewModel::class.java)
    }
}