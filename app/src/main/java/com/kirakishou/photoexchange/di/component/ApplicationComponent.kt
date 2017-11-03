package com.kirakishou.photoexchange.di.component

import android.content.Context
import com.kirakishou.photoexchange.di.module.ApplicationModule
import com.kirakishou.photoexchange.di.module.GsonModule
import com.kirakishou.photoexchange.di.module.NetworkModule
import com.kirakishou.photoexchange.helper.api.ApiService
import dagger.Component
import javax.inject.Singleton

/**
 * Created by kirakishou on 11/3/2017.
 */

@Singleton
@Component(modules = arrayOf(ApplicationModule::class, NetworkModule::class, GsonModule::class))
interface ApplicationComponent {
    fun exposeContext(): Context
    fun exposeApiService(): ApiService
}