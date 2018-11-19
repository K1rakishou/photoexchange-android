package com.kirakishou.photoexchange.ui.viewstate

data class UploadedPhotosFragmentViewState(
  var lastUploadedOn: Long = -1,
  var photosPerPage: Int = 0,
  var failedPhotosLoaded: Boolean = false
) {

  fun reset() {
    lastUploadedOn = -1
    lastUploadedOn = 0
    failedPhotosLoaded = false
  }
}