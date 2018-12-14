package com.kirakishou.photoexchange.di.module

import android.content.Context
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.util.*
import dagger.Module
import dagger.Provides
import javax.inject.Named
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
  fun provideNetUtils(@Named("app_context") context: Context,
                      settingsRepository: SettingsRepository,
                      dispatchersProvider: DispatchersProvider): NetUtils {
    return NetUtilsImpl(
      context,
      settingsRepository,
      dispatchersProvider
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

  @Provides
  @Singleton
  fun providePhotoAdditionalInfoUtils(netUtils: NetUtils): PhotoAdditionalInfoUtils {
    return PhotoAdditionalInfoUtilsImpl(
      netUtils
    )
  }
}