package com.kirakishou.photoexchange.di.module.fragment

import com.kirakishou.photoexchange.di.scope.PerFragment
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.ui.epoxy.controller.ReceivedPhotosFragmentEpoxyController
import dagger.Module
import dagger.Provides

@Module
class ReceivedPhotosFragmentModule {

  @PerFragment
  @Provides
  fun provideReceivedPhotosFragmentEpoxyController(
    imageLoader: ImageLoader
  ): ReceivedPhotosFragmentEpoxyController {
    return ReceivedPhotosFragmentEpoxyController(imageLoader)
  }

}