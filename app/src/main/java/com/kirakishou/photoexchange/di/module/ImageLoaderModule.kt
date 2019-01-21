package com.kirakishou.photoexchange.di.module

import android.content.Context
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.util.NetUtils
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

/**
 * Created by kirakishou on 3/11/2018.
 */
@Module
class ImageLoaderModule {

  @Singleton
  @Provides
  fun provideImageLoader(@Named("app_context") context: Context,
                         netUtils: NetUtils,
                         dispatchersProvider: DispatchersProvider): ImageLoader {
    return ImageLoader(
      context,
      netUtils,
      dispatchersProvider
    )
  }
}