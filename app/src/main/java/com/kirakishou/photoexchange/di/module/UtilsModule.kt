package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.helper.util.*
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class UtilsModule {

    @Provides
    @Singleton
    fun provideTimeUtils(): TimeUtils {
        return TimeUtilsImpl()
    }

    @Provides
    @Singleton
    fun provideBitmapUtils(): BitmapUtils {
        return BitmapUtilsImpl()
    }

    @Provides
    @Singleton
    fun provideFileUtils(): FileUtils {
        return FileUtilsImpl()
    }
}