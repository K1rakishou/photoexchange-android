package com.kirakishou.photoexchange.di.component

import android.content.Context
import android.content.SharedPreferences
import com.kirakishou.photoexchange.di.module.*
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.repository.database.MyDatabase
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import dagger.Component
import javax.inject.Singleton

/**
 * Created by kirakishou on 11/3/2017.
 */

@Singleton
@Component(modules = arrayOf(
        ApplicationModule::class,
        NetworkModule::class,
        GsonModule::class,
        ApiClientModule::class,
        SchedulerProviderModule::class,
        AppSharedPreferenceModule::class,
        DatabaseModule::class))
interface ApplicationComponent {
    fun exposeContext(): Context
    fun exposeApiService(): ApiService
    fun exposeApiClient(): ApiClient
    fun exposeSchedulers(): SchedulerProvider
    fun exposeSharedPreferences(): SharedPreferences
    fun exposeDatabase(): MyDatabase
}