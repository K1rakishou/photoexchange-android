package com.kirakishou.photoexchange.dagger.module

import com.kirakishou.photoexchange.helper.api.ApiClient
import dagger.Module
import dagger.Provides
import org.mockito.Mockito
import javax.inject.Singleton

/**
 * Created by kirakishou on 3/17/2018.
 */
@Module
class MockApiClientModule {

  @Singleton
  @Provides
  fun provideApiClient(): ApiClient {
    return Mockito.mock(ApiClient::class.java)
  }
}