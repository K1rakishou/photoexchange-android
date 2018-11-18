package com.kirakishou.photoexchange.di.module

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.kirakishou.photoexchange.helper.gson.JsonConverter
import com.kirakishou.photoexchange.helper.gson.JsonConverterImpl
import dagger.Module
import dagger.Provides
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * Created by kirakishou on 3/17/2018.
 */
@Module
class GsonModule {

  @Singleton
  @Provides
  fun provideGson(): Gson {
    return GsonBuilder()
      .excludeFieldsWithoutExposeAnnotation()
      .setPrettyPrinting()
      .create()
  }

  @Singleton
  @Provides
  fun provideGsonConverterFactory(gson: Gson): GsonConverterFactory {
    return GsonConverterFactory.create(gson)
  }

  @Singleton
  @Provides
  fun provideJsonConverter(gson: Gson): JsonConverter {
    return JsonConverterImpl(gson)
  }
}