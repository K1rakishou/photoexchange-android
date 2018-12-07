package com.kirakishou.photoexchange.di.module

import android.content.Context
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
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

  @Provides
  @Singleton
  fun provideNetUtils(context: Context,
                      settingsRepository: SettingsRepository): NetUtils {
    return NetUtilsImpl(
      context,
      settingsRepository
    )
  }

  @Provides
  @Singleton
  fun providePagedApiUtils(timeUtils: TimeUtils,
                           netUtils: NetUtils): PagedApiUtils {
    return PagedApiUtilsImpl(
      timeUtils,
      netUtils
    )
  }
}