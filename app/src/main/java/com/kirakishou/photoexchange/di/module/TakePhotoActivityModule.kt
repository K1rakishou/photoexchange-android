package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.database.repository.MyPhotoRepository
import com.kirakishou.photoexchange.mvp.view.TakePhotoActivityView
import com.kirakishou.photoexchange.mvp.viewmodel.factory.TakePhotoActivityViewModelFactory
import dagger.Module
import dagger.Provides

/**
 * Created by kirakishou on 3/3/2018.
 */

@Module
open class TakePhotoActivityModule(
    val view: TakePhotoActivityView
) {

    @PerActivity
    @Provides
    open fun provideViewModelFactory(coroutinesPool: CoroutineThreadPoolProvider,
                                myPhotoRepository: MyPhotoRepository): TakePhotoActivityViewModelFactory {
        return TakePhotoActivityViewModelFactory(view, myPhotoRepository, coroutinesPool)
    }
}