package com.kirakishou.photoexchange.ui.viewstate

import com.kirakishou.photoexchange.mvp.model.photo.UploadedPhoto

data class UploadedPhotosFragmentViewState(
  private var lastUploadedOn: Long = -1,
  var photosPerPage: Int = 0,
  var failedPhotosLoaded: Boolean = false
) {

  fun getLastUploadedOn(): Long = lastUploadedOn

  fun updateFromUploadedPhotos(uploadedPhotos: List<UploadedPhoto>) {
    if (uploadedPhotos.isEmpty()) {
      return
    }

    lastUploadedOn = uploadedPhotos.last().uploadedOn
  }

  fun reset() {
    lastUploadedOn = -1
    lastUploadedOn = 0
    failedPhotosLoaded = false
  }
}