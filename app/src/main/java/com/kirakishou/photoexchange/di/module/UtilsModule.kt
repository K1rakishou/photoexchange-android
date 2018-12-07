package com.kirakishou.photoexchange.di.module

import android.content.Context
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
  fun provideNetUtils(context: Context): NetUtils {
    return NetUtilsImpl(context)
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