package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.api.ApiClientImpl
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.gson.MyGson
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by kirakishou on 3/17/2018.
 */
@Module(includes = [
    NetworkModule::class,
    SchedulerProviderModule::class
])
class ApiClientModule {

    @Singleton
    @Provides
    fun provideApiClient(apiService: ApiService, gson: MyGson, schedulerProvider: SchedulerProvider): ApiClient {
        return ApiClientImpl(apiService, gson, schedulerProvider)
    }
}