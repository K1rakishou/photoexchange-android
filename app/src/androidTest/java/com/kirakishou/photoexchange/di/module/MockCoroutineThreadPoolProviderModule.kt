package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.android.UI
import javax.inject.Singleton

/**
 * Created by kirakishou on 3/8/2018.
 */

@Module
class MockCoroutineThreadPoolProviderModule {

    @Singleton
    @Provides
    fun provideCoroutineThreadPoolProvider(): CoroutineThreadPoolProvider {
        return TestCoroutineThreadPoolProvider()
    }

    class TestCoroutineThreadPoolProvider : CoroutineThreadPoolProvider {
        override fun provideCommon(): CoroutineDispatcher = Unconfined
        override fun provideDb(): CoroutineDispatcher = Unconfined
        override fun provideNetwork(): CoroutineDispatcher = Unconfined
        override fun provideUi(): CoroutineDispatcher = Unconfined
    }
}