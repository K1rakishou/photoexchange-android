package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.mvvm.viewmodel.factory.AllPhotoViewActivityViewModelFactory
import com.kirakishou.photoexchange.ui.activity.AllPhotoViewActivity
import com.kirakishou.photoexchange.ui.navigator.AllPhotoViewActivityNavigator
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by kirakishou on 11/7/2017.
 */

@Module
class AllPhotoViewActivityModule(val activity: AllPhotoViewActivity) {

    @PerActivity
    @Provides
    fun provideNavigator(): AllPhotoViewActivityNavigator {
        return AllPhotoViewActivityNavigator(activity)
    }

    @PerActivity
    @Provides
    fun provideViewModelFactory(): AllPhotoViewActivityViewModelFactory {
        return AllPhotoViewActivityViewModelFactory()
    }
}