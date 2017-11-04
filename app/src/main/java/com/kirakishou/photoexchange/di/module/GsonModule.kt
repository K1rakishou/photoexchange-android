package com.kirakishou.photoexchange.di.module

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.kirakishou.photoexchange.helper.util.gson.LonLatGsonTypeAdapter
import com.kirakishou.photoexchange.mvvm.model.LonLat
import dagger.Module
import dagger.Provides
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * Created by kirakishou on 11/3/2017.
 */

@Module
class GsonModule {

    @Singleton
    @Provides
    fun provideGson(): Gson {
        return GsonBuilder()
                .registerTypeAdapter(LonLat::class.java, LonLatGsonTypeAdapter())
                .excludeFieldsWithoutExposeAnnotation()
                .setPrettyPrinting()
                .create()
    }

    @Singleton
    @Provides
    fun provideGsonConverterFactory(gson: Gson): GsonConverterFactory {
        return GsonConverterFactory.create(gson)
    }
}