package com.kirakishou.photoexchange.di.module

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by kirakishou on 11/3/2017.
 */

@Module
class ApplicationModule(private val application: Application,
                        private val databaseName: String) {

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
}