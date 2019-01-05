package com.kirakishou.photoexchange.dagger.module

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.concurrency.coroutines.NormalDispatchers
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
open class MockDispatchersProviderModule {

  @Singleton
  @Provides
  fun provideDispatchers(): DispatchersProvider {
    return NormalDispatchers()
  }
}