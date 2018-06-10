package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.helper.util.TimeUtilsImpl
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class TimeUtilsModule {

    @Provides
    @Singleton
    fun provideTimeUtils(): TimeUtils {
        return TimeUtilsImpl()
    }
}