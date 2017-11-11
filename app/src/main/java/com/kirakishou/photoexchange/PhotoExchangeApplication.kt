package com.kirakishou.photoexchange

import android.app.Application
import com.kirakishou.photoexchange.di.component.ApplicationComponent
import com.kirakishou.photoexchange.di.component.DaggerApplicationComponent
import com.kirakishou.photoexchange.di.module.*
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import timber.log.Timber
import android.os.StrictMode



/**
 * Created by kirakishou on 11/3/2017.
 */

class PhotoExchangeApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        applicationComponent = DaggerApplicationComponent
                .builder()
                .applicationModule(ApplicationModule(this))
                .networkModule(NetworkModule(baseUrl))
                .gsonModule(GsonModule())
                .apiClientModule(ApiClientModule())
                .schedulerProviderModule(SchedulerProviderModule())
                .appSharedPreferenceModule(AppSharedPreferenceModule())
                .databaseModule(DatabaseModule(databaseName))
                .mapperModule(MapperModule())
                .eventBusModule(EventBusModule())
                .build()

        initTimber()
        initLeakCanary()
        enabledStrictMode()
    }

    private fun initLeakCanary() {
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return
        }

        refWatcher = LeakCanary.install(this)
    }

    private fun enabledStrictMode() {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder() //
                .detectAll() //
                .penaltyLog() //
                .penaltyDeath() //
                .build())
    }

    private fun initTimber() {
        Timber.plant(Timber.DebugTree())
    }

    companion object {
        @JvmStatic lateinit var applicationComponent: ApplicationComponent
        lateinit var refWatcher: RefWatcher
        val baseUrl = "http://kez1911.asuscomm.com:8080/"
        val databaseName = "photoexchange_db"
    }
}