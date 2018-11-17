package com.kirakishou.photoexchange.ui.viewstate

class GalleryFragmentViewState(
  var lastUploadedOn: Long = -1,
  var count: Int = 5
) {

  fun updateLastUploadedOn(newLastUploadedOn: Long?) {
    if (newLastUploadedOn == null) {
      return
    }

    lastUploadedOn = newLastUploadedOn
  }

  fun updateCount(newCount: Int) {
    count = newCount
  }

  fun reset() {
    lastUploadedOn = -1
  }
}