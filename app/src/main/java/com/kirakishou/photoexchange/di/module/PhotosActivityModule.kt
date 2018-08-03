package com.kirakishou.photoexchange.di.module

import android.arch.lifecycle.ViewModelProviders
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.*
import com.kirakishou.photoexchange.interactors.*
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.viewmodel.GalleryFragmentViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.ReceivedPhotosFragmentViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.UploadedPhotosFragmentViewModel
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
    fun provideUploadedPhotosFragmentViewModel(takenPhotosRepository: TakenPhotosRepository,
                                               settingsRepository: SettingsRepository,
                                               getUploadedPhotosUseCase: GetUploadedPhotosUseCase,
                                               schedulerProvider: SchedulerProvider): UploadedPhotosFragmentViewModel {
        return UploadedPhotosFragmentViewModel(
            takenPhotosRepository,
            settingsRepository,
            getUploadedPhotosUseCase,
            schedulerProvider,
            Constants.ADAPTER_LOAD_MORE_ITEMS_DELAY_MS,
            Constants.PROGRESS_FOOTER_REMOVE_DELAY_MS
        )
    }

    @PerActivity
    @Provides
    fun provideReceivedPhotosFragmentViewModel(settingsRepository: SettingsRepository,
                                               getReceivedPhotosUseCase: GetReceivedPhotosUseCase,
                                               schedulerProvider: SchedulerProvider): ReceivedPhotosFragmentViewModel {
        return ReceivedPhotosFragmentViewModel(
            settingsRepository,
            getReceivedPhotosUseCase,
            schedulerProvider,
            Constants.ADAPTER_LOAD_MORE_ITEMS_DELAY_MS,
            Constants.PROGRESS_FOOTER_REMOVE_DELAY_MS
        )
    }

    @PerActivity
    @Provides
    fun provideGalleryFragmentViewModel(imageLoader: ImageLoader,
                                        settingsRepository: SettingsRepository,
                                        galleryPhotosUseCase: GetGalleryPhotosUseCase,
                                        getGalleryPhotosInfoUseCase: GetGalleryPhotosInfoUseCase,
                                        schedulerProvider: SchedulerProvider): GalleryFragmentViewModel {
        return GalleryFragmentViewModel(
            imageLoader,
            settingsRepository,
            galleryPhotosUseCase,
            getGalleryPhotosInfoUseCase,
            schedulerProvider,
            Constants.ADAPTER_LOAD_MORE_ITEMS_DELAY_MS,
            Constants.PROGRESS_FOOTER_REMOVE_DELAY_MS
        )
    }

    @PerActivity
    @Provides
    fun provideViewModelFactory(settingsRepository: SettingsRepository,
                                takenPhotosRepository: TakenPhotosRepository,
                                uploadedPhotosRepository: UploadedPhotosRepository,
                                receivedPhotosRepository: ReceivedPhotosRepository,
                                uploadedPhotosFragmentViewModel: UploadedPhotosFragmentViewModel,
                                receivedPhotosFragmentViewModel: ReceivedPhotosFragmentViewModel,
                                galleryFragmentViewModel: GalleryFragmentViewModel,
                                reportPhotoUseCase: ReportPhotoUseCase,
                                favouritePhotoUseCase: FavouritePhotoUseCase,
                                schedulerProvider: SchedulerProvider): PhotosActivityViewModelFactory {
        return PhotosActivityViewModelFactory(
            settingsRepository,
            takenPhotosRepository,
            uploadedPhotosRepository,
            receivedPhotosRepository,
            uploadedPhotosFragmentViewModel,
            receivedPhotosFragmentViewModel,
            galleryFragmentViewModel,
            reportPhotoUseCase,
            favouritePhotoUseCase,
            schedulerProvider)
    }

    @PerActivity
    @Provides
    fun provideViewModel(viewModelFactory: PhotosActivityViewModelFactory): PhotosActivityViewModel {
        return ViewModelProviders.of(activity, viewModelFactory).get(PhotosActivityViewModel::class.java)
    }
}