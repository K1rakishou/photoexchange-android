package com.kirakishou.photoexchange.ui.viewstate

import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhoto

class GalleryFragmentViewState(
  private var lastUploadedOn: Long = -1,
  var count: Int = 5
) {

  fun getLastUploadedOn(): Long = lastUploadedOn

  fun updateFromGalleryPhotos(galleryPhotos: List<GalleryPhoto>) {
    if (galleryPhotos.isEmpty()) {
      return
    }

    lastUploadedOn = galleryPhotos.last().uploadedOn
  }

  fun updateCount(newCount: Int) {
    count = newCount
  }

  fun reset() {
    lastUploadedOn = -1
  }
}