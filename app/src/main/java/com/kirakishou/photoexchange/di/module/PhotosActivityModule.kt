package com.kirakishou.photoexchange.di.module

import android.arch.lifecycle.ViewModelProviders
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.interactors.FavouritePhotoUseCase
import com.kirakishou.photoexchange.interactors.GetGalleryPhotosUseCase
import com.kirakishou.photoexchange.interactors.ReportPhotoUseCase
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.factory.AllPhotosActivityViewModelFactory
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
    fun provideViewModelFactory(schedulerProvider: SchedulerProvider,
                                photosRepository: PhotosRepository,
                                receivedPhotosRepository: ReceivedPhotosRepository,
                                settingsRepository: SettingsRepository,
                                galleryPhotosUseCase: GetGalleryPhotosUseCase,
                                favouritePhotoUseCase: FavouritePhotoUseCase,
                                reportPhotoUseCase: ReportPhotoUseCase): AllPhotosActivityViewModelFactory {
        return AllPhotosActivityViewModelFactory(
            photosRepository,
            settingsRepository,
            receivedPhotosRepository,
            galleryPhotosUseCase,
            favouritePhotoUseCase,
            reportPhotoUseCase,
            schedulerProvider)
    }

    @PerActivity
    @Provides
    fun provideViewModel(viewModelFactory: AllPhotosActivityViewModelFactory): PhotosActivityViewModel {
        return ViewModelProviders.of(activity, viewModelFactory).get(PhotosActivityViewModel::class.java)
    }
}