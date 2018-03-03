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
class CoroutineThreadPoolProviderModule {

    @Singleton
    @Provides
    fun provideCoroutineThreadPoolProvider(): CoroutineThreadPoolProvider {
        return NormalCoroutineThreadPoolProvider()
    }
}