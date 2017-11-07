package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.mvvm.viewmodel.factory.AllPhotosViewActivityViewModelFactory
import com.kirakishou.photoexchange.ui.activity.AllPhotosViewActivity
import com.kirakishou.photoexchange.ui.navigator.AllPhotoViewActivityNavigator
import dagger.Module
import dagger.Provides

/**
 * Created by kirakishou on 11/7/2017.
 */

@Module
class AllPhotoViewActivityModule(val activity: AllPhotosViewActivity) {

    @PerActivity
    @Provides
    fun provideNavigator(): AllPhotoViewActivityNavigator {
        return AllPhotoViewActivityNavigator(activity)
    }

    @PerActivity
    @Provides
    fun provideViewModelFactory(): AllPhotosViewActivityViewModelFactory {
        return AllPhotosViewActivityViewModelFactory()
    }
}