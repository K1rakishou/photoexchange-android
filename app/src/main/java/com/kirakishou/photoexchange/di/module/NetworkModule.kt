package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.Constants
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Created by kirakishou on 3/17/2018.
 */
@Module
class NetworkModule(private val baseUrl: String) {

  @Singleton
  @Provides
  fun provideLoggingInterceptor(): HttpLoggingInterceptor {
    val loggingInterceptor = HttpLoggingInterceptor()
    loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

    return loggingInterceptor
  }

  @Singleton
  @Provides
  fun provideOkHttpClient(): OkHttpClient {
    return if (Constants.isDebugBuild) {
      OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
    } else {
      OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    }
  }

  @Singleton
  @Provides
  fun provideRetrofit(
    client: OkHttpClient,
    converterFactory: GsonConverterFactory
  ): Retrofit {
    return Retrofit.Builder()
      .baseUrl(baseUrl)
      .addConverterFactory(converterFactory)
      .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
      .client(client)
      .build()
  }

  @Singleton
  @Provides
  fun provideApiService(retrofit: Retrofit): ApiService {
    return retrofit.create(ApiService::class.java)
  }
}