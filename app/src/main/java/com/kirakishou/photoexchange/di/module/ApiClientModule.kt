package com.kirakishou.photoexchange.di.module

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by kirakishou on 3/17/2018.
 */
@Module(includes = [
    NetworkModule::class,
    CoroutineThreadPoolProviderModule::class
])
class ApiClientModule {

    @Singleton
    @Provides
    fun provideApiClient(apiService: ApiService, gson: Gson, coroutinePool: CoroutineThreadPoolProvider): ApiClient {
        return ApiClient(apiService, gson, coroutinePool)
    }
}