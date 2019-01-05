package com.kirakishou.photoexchange.dagger.module

import com.kirakishou.photoexchange.helper.util.*
import dagger.Module
import dagger.Provides
import org.mockito.Mockito
import javax.inject.Singleton

@Module
class MockUtilsModule {

  @Provides
  @Singleton
  fun provideTimeUtils(): TimeUtils {
    return Mockito.spy(TimeUtilsImpl())
  }

  @Provides
  @Singleton
  fun provideBitmapUtils(): BitmapUtils {
    return Mockito.spy(BitmapUtilsImpl())
  }

  @Provides
  @Singleton
  fun provideFileUtils(): FileUtils {
    return Mockito.spy(FileUtilsImpl())
  }

  @Provides
  @Singleton
  fun provideNetUtils(): NetUtils {
    return Mockito.mock(NetUtils::class.java)
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