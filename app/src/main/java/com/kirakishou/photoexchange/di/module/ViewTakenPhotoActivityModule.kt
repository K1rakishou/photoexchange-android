package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.database.repository.MyPhotoRepository
import com.kirakishou.photoexchange.mvp.view.ViewTakenPhotoActivityView
import com.kirakishou.photoexchange.mvp.viewmodel.factory.ViewTakenPhotoActivityViewModelFactory
import dagger.Module
import dagger.Provides

/**
 * Created by kirakishou on 3/9/2018.
 */

@Module
open class ViewTakenPhotoActivityModule(
    val view: ViewTakenPhotoActivityView
) {

    @PerActivity
    @Provides
    open fun provideViewModelFactory(coroutinePool: CoroutineThreadPoolProvider,
                                     myPhotoRepository: MyPhotoRepository): ViewTakenPhotoActivityViewModelFactory {
        return ViewTakenPhotoActivityViewModelFactory(view, coroutinePool, myPhotoRepository)
    }
}