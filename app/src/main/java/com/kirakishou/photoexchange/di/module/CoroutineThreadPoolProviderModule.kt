package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.concurrency.coroutine.NormalCoroutineThreadPoolProvider
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by kirakishou on 3/3/2018.
 */

@Module
open class CoroutineThreadPoolProviderModule {

    @Singleton
    @Provides
    open fun provideCoroutineThreadPoolProvider(): CoroutineThreadPoolProvider {
        return NormalCoroutineThreadPoolProvider()
    }
}