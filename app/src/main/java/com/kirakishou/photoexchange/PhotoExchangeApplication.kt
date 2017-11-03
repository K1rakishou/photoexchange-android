package com.kirakishou.photoexchange

import android.app.Application
import com.kirakishou.photoexchange.di.component.ApplicationComponent
import com.kirakishou.photoexchange.di.component.DaggerApplicationComponent
import com.kirakishou.photoexchange.di.module.ApplicationModule
import com.kirakishou.photoexchange.di.module.GsonModule
import com.kirakishou.photoexchange.di.module.NetworkModule
import timber.log.Timber

/**
 * Created by kirakishou on 11/3/2017.
 */

class PhotoExchangeApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        applicationComponent = DaggerApplicationComponent
                .builder()
                .applicationModule(ApplicationModule(this, databaseName))
                .networkModule(NetworkModule(baseUrl))
                .gsonModule(GsonModule())
                .build()

        initTimber()
    }

    private fun initTimber() {
        Timber.plant(Timber.DebugTree())
    }

    companion object {
        @JvmStatic lateinit var applicationComponent: ApplicationComponent
        private val baseUrl = "http://kez1911.asuscomm.com:8080/"
        private val databaseName = "photoexchange_db"
    }
}