package com.kirakishou.photoexchange.di.component

import android.content.Context
import android.content.SharedPreferences
import com.kirakishou.photoexchange.di.module.*
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.PhotoAnswerRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import dagger.Component
import org.greenrobot.eventbus.EventBus
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
        DatabaseModule::class,
        MapperModule::class,
        EventBusModule::class))
interface ApplicationComponent {
    fun exposeContext(): Context
    fun exposeApiService(): ApiService
    fun exposeApiClient(): ApiClient
    fun exposeSchedulers(): SchedulerProvider
    fun exposeSharedPreferences(): SharedPreferences
    fun exposeDatabase(): MyDatabase
    fun exposeTakenPhotosRepository(): TakenPhotosRepository
    fun exposePhotoAnswerRepository(): PhotoAnswerRepository
    fun exposeEventBust(): EventBus
}