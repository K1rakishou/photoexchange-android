package com.kirakishou.photoexchange.di.module

import android.arch.lifecycle.ViewModelProviders
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.mvp.viewmodel.ViewTakenPhotoActivityViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.factory.ViewTakenPhotoActivityViewModelFactory
import com.kirakishou.photoexchange.ui.activity.ViewTakenPhotoActivity
import dagger.Module
import dagger.Provides
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 3/9/2018.
 */

@Module
open class ViewTakenPhotoActivityModule(
    val activity: ViewTakenPhotoActivity
) {

    @PerActivity
    @Provides
    open fun provideViewModelFactory(schedulerProvider: SchedulerProvider,
                                     photosRepository: PhotosRepository,
                                     settingsRepository: SettingsRepository): ViewTakenPhotoActivityViewModelFactory {
        return ViewTakenPhotoActivityViewModelFactory(schedulerProvider, photosRepository, settingsRepository)
    }

    @PerActivity
    @Provides
    fun provideViewModel(viewModelFactory: ViewTakenPhotoActivityViewModelFactory): ViewTakenPhotoActivityViewModel {
        return ViewModelProviders.of(activity, viewModelFactory).get(ViewTakenPhotoActivityViewModel::class.java)
    }
}