package com.kirakishou.photoexchange.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by kirakishou on 11/5/2017.
 */

@Module
class ServiceModule(val context: Context) {

    @Singleton
    @Provides
    fun provideContext(): Context {
        return context
    }


}