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
import com.kirakishou.photoexchange.mwvm.model.other.Constants


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
                .imageLoaderModule(ImageLoaderModule())
                .eventAccumulatorModule(EventAccumulatorModule())
                .build()

        initTimber()
        initLeakCanary()
        //enabledStrictMode()
    }

    private fun initLeakCanary() {
        if (Constants.isDebugBuild) {
            if (LeakCanary.isInAnalyzerProcess(this)) {
                return
            }

            refWatcher = LeakCanary.install(this)
        }
    }

    private fun enabledStrictMode() {
        if (Constants.isDebugBuild) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    //.penaltyDeath()
                    .build())

            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    //.penaltyDeath()
                    .build())
        }
    }

    private fun initTimber() {
        if (Constants.isDebugBuild) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashlyticsTree())
        }
    }

    class CrashlyticsTree : Timber.Tree() {
        override fun log(priority: Int, tagParam: String?, messageParam: String?, error: Throwable?) {
            if (priority != Log.WARN && priority != Log.ERROR && priority != Log.DEBUG) {
                return
            }

            val tag = tagParam ?: "Empty tag"

            Crashlytics.setInt(CRASHLYTICS_KEY_PRIORITY, priority)
            Crashlytics.setString(CRASHLYTICS_KEY_TAG, tag)

            when (priority) {
                Log.DEBUG -> {
                    if (messageParam != null) {
                        Crashlytics.log(Log.DEBUG, tag, messageParam)
                    }
                }

                Log.WARN -> {
                    if (error == null) {
                        if (messageParam != null) {
                            Crashlytics.log(Log.WARN, tag, messageParam)
                        }
                    } else {
                        Crashlytics.logException(error)
                    }
                }

                Log.ERROR -> {
                    if (error == null) {
                        if (messageParam != null) {
                            Crashlytics.log(Log.ERROR, tag, messageParam)
                        }
                    } else {
                        Crashlytics.logException(error)
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic lateinit var applicationComponent: ApplicationComponent
        var refWatcher: RefWatcher? = null
        val baseUrl = "http://kez1911.asuscomm.com:8080/"
        val databaseName = "photoexchange_db"

        private val CRASHLYTICS_KEY_PRIORITY = "priority"
        private val CRASHLYTICS_KEY_TAG = "tag"
        private val CRASHLYTICS_KEY_MESSAGE = "message"

        fun watch(reference: Any, refName: String?) {
            if (refWatcher != null) {
                refWatcher!!.watch(reference, refName ?: "Unknown class")
            }
        }
    }
}