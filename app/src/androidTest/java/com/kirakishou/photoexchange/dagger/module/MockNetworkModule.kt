package com.kirakishou.photoexchange.dagger.module

import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.Constants
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.mockito.Mockito
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Created by kirakishou on 3/17/2018.
 */
@Module
class MockNetworkModule() {

  @Singleton
  @Provides
  fun provideApiService(retrofit: Retrofit): ApiService {
    return Mockito.mock(ApiService::class.java)
  }
}