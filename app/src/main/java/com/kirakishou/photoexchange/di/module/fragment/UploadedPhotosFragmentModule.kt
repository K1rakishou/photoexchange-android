package com.kirakishou.photoexchange.di.module.fragment

import com.kirakishou.photoexchange.di.scope.PerFragment
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.ui.epoxy.controller.UploadedPhotosFragmentEpoxyController
import dagger.Module
import dagger.Provides

@Module
class UploadedPhotosFragmentModule {

  @PerFragment
  @Provides
  fun provideUploadedPhotosFragmentEpoxyController(
    imageLoader: ImageLoader
  ): UploadedPhotosFragmentEpoxyController {
    return UploadedPhotosFragmentEpoxyController(imageLoader)
  }

}