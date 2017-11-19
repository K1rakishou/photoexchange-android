package com.kirakishou.photoexchange

import android.app.Application
import com.kirakishou.photoexchange.di.component.ApplicationComponent
import com.kirakishou.photoexchange.di.component.DaggerApplicationComponent
import com.kirakishou.photoexchange.di.module.*
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import timber.log.Timber
import android.os.StrictMode
import android.util.Log
import com.crashlytics.android.Crashlytics


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
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .penaltyDeath()
                .build())
    }

    private fun initTimber() {
        Timber.plant(CrashlyticsTree())
    }

    class CrashlyticsTree : Timber.Tree() {
        override fun log(priority: Int, tagParam: String?, messageParam: String?, t: Throwable?) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG || priority == Log.INFO) {
                if (messageParam != null) {
                    println(messageParam)
                }
                return
            }

            if (priority == Log.WARN) {
                if (messageParam != null) {
                    System.err.println(messageParam)
                }
                return
            }

            val tag = tagParam ?: "Empty tag"
            val message = messageParam ?: "Empty message"

            Crashlytics.setInt(CRASHLYTICS_KEY_PRIORITY, priority)
            Crashlytics.setString(CRASHLYTICS_KEY_TAG, tag)
            Crashlytics.setString(CRASHLYTICS_KEY_MESSAGE, message)

            if (t == null) {
                println(message)
            } else {
                Crashlytics.logException(t)
                t.printStackTrace()
            }
        }
    }

    companion object {
        @JvmStatic lateinit var applicationComponent: ApplicationComponent
        lateinit var refWatcher: RefWatcher
        val baseUrl = "http://kez1911.asuscomm.com:8080/"
        val databaseName = "photoexchange_db"

        private val CRASHLYTICS_KEY_PRIORITY = "priority"
        private val CRASHLYTICS_KEY_TAG = "tag"
        private val CRASHLYTICS_KEY_MESSAGE = "message"
    }
}