package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.mvvm.viewmodel.factory.TakePhotoActivityViewModelFactory
import dagger.Module
import dagger.Provides

/**
 * Created by kirakishou on 11/3/2017.
 */

@Module
class TakePhotoActivityModule {

    @PerActivity
    @Provides
    fun provideViewModelFactory(takenPhotosRepository: TakenPhotosRepository): TakePhotoActivityViewModelFactory {
        return TakePhotoActivityViewModelFactory(takenPhotosRepository)
    }
}