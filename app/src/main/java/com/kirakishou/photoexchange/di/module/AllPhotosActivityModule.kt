package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.mvp.view.AllPhotosActivityView
import com.kirakishou.photoexchange.mvp.viewmodel.factory.AllPhotosActivityViewModelFactory
import dagger.Module
import dagger.Provides

/**
 * Created by kirakishou on 3/11/2018.
 */

@Module
open class AllPhotosActivityModule(
    val view: AllPhotosActivityView
) {

    @PerActivity
    @Provides
    fun provideViewModelFactory(coroutinePool: CoroutineThreadPoolProvider,
                                photosRepository: PhotosRepository,
                                settingsRepository: SettingsRepository): AllPhotosActivityViewModelFactory {
        return AllPhotosActivityViewModelFactory(view, photosRepository, settingsRepository, coroutinePool)
    }
}