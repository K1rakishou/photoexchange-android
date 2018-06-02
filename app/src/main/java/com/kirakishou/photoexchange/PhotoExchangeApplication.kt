package com.kirakishou.photoexchange

import android.app.Application
import com.kirakishou.photoexchange.di.component.DaggerApplicationComponent
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import timber.log.Timber
import android.os.StrictMode
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.kirakishou.photoexchange.di.component.ApplicationComponent
import com.kirakishou.photoexchange.di.module.ApplicationModule
import com.kirakishou.photoexchange.di.module.DatabaseModule
import com.kirakishou.photoexchange.di.module.NetworkModule
import com.kirakishou.photoexchange.di.module.SchedulerProviderModule
import com.kirakishou.photoexchange.mvp.model.other.Constants
import io.fabric.sdk.android.Fabric
import com.crashlytics.android.core.CrashlyticsCore
import com.facebook.stetho.Stetho
import com.kirakishou.fixmypc.photoexchange.BuildConfig




/**
 * Created by kirakishou on 11/3/2017.
 */

open class PhotoExchangeApplication : Application() {

    open lateinit var applicationComponent: ApplicationComponent

    override fun onCreate() {
        super.onCreate()

        init()

        applicationComponent = initializeApplicationComponent()
        applicationComponent.inject(this)
    }

    open fun initializeApplicationComponent(): ApplicationComponent {
        return DaggerApplicationComponent.builder()
            .applicationModule(ApplicationModule(this))
            .databaseModule(DatabaseModule(Constants.DATABASE_NAME))
            .networkModule(NetworkModule(Constants.BASE_URL))
            .schedulerProviderModule(SchedulerProviderModule())
            .build()
    }

    open fun init() {
        Stetho.initializeWithDefaults(this);

        initCrashlytics()
        initTimber()
        initLeakCanary()
        //enabledStrictMode()
    }

    private fun initCrashlytics() {
        val crashlyticsKit = Crashlytics.Builder()
            .core(CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
            .build()

        Fabric.with(this, crashlyticsKit)
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
            Timber.plant(DebugTree())
        } else {
            Timber.plant(CrashlyticsTree())
        }
    }

    class DebugTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, error: Throwable?) {
            when (priority) {
                Log.DEBUG -> {
                    System.out.println("_DEBUG: ($tag) $message")
                }

                Log.WARN -> {
                    System.out.println("_WARNING: ($tag) $message")
                }

                Log.ERROR -> {
                    System.err.println("_ERROR: ($tag) $message")
                    error?.printStackTrace()
                }

                else -> {
                    System.out.println("_OTHER: ($tag) $message")
                }
            }
        }
    }

    class CrashlyticsTree : Timber.Tree() {
        override fun log(priority: Int, tagParam: String?, messageParam: String, error: Throwable?) {
            if (priority != Log.WARN && priority != Log.ERROR && priority != Log.DEBUG) {
                return
            }

            val tag = tagParam ?: "Empty tag"

            Crashlytics.setInt(CRASHLYTICS_KEY_PRIORITY, priority)
            Crashlytics.setString(CRASHLYTICS_KEY_TAG, tag)

            when (priority) {
                Log.DEBUG -> {
                    Crashlytics.log(Log.DEBUG, tag, messageParam)
                }

                Log.WARN -> {
                    if (error == null) {
                        Crashlytics.log(Log.WARN, tag, messageParam)
                    } else {
                        Crashlytics.logException(error)
                    }
                }

                Log.ERROR -> {
                    if (error == null) {
                        Crashlytics.log(Log.ERROR, tag, messageParam)
                    } else {
                        Crashlytics.logException(error)
                    }
                }
            }
        }
    }

    companion object {
        var refWatcher: RefWatcher? = null
        val baseUrl = "http://kez1911.asuscomm.com:8080/"

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