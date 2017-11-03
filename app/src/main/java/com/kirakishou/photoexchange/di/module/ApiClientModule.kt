package com.kirakishou.photoexchange.di.module

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.api.ApiClientImpl
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by kirakishou on 11/3/2017.
 */

@Module
class ApiClientModule {

    @Singleton
    @Provides
    fun provideApiClient(apiService: ApiService, gson: Gson, schedulers: SchedulerProvider): ApiClient {
        return ApiClientImpl(apiService, gson, schedulers)
    }
}