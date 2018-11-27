package com.kirakishou.photoexchange.mvp.viewmodel.state

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhoto

data class GalleryFragmentState(
  val isEndReached: Boolean = false,
  val galleryPhotos: List<GalleryPhoto> = emptyList(),
  val galleryPhotosRequest: Async<List<GalleryPhoto>> = Uninitialized
) : MvRxState