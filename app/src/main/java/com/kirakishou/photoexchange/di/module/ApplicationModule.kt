package com.kirakishou.photoexchange.di.module

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.kirakishou.photoexchange.mwvm.model.other.Constants
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by kirakishou on 11/3/2017.
 */

@Module
class ApplicationModule(private val application: Application) {

    @Singleton
    @Provides
    fun provideApplication(): Application {
        return application
    }

    @Singleton
    @Provides
    fun provideContext(): Context {
        return application.applicationContext
    }

    @Singleton
    @Provides
    fun provideSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(Constants.SHARED_PREFS_PREFIX, Context.MODE_PRIVATE)
    }
}