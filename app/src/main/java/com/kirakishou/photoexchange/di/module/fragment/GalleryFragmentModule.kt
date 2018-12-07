package com.kirakishou.photoexchange.di.module.fragment

import com.kirakishou.photoexchange.di.scope.PerFragment
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.ui.epoxy.controller.GalleryFragmentEpoxyController
import dagger.Module
import dagger.Provides

@Module
class GalleryFragmentModule {

  @PerFragment
  @Provides
  fun provideGalleryFragmentEpoxyController(
    imageLoader: ImageLoader
  ): GalleryFragmentEpoxyController {
    return GalleryFragmentEpoxyController(imageLoader)
  }

}