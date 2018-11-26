package com.kirakishou.photoexchange.ui.viewstate

import com.kirakishou.photoexchange.mvp.model.photo.ReceivedPhoto

class ReceivedPhotosFragmentViewState(
  private var lastUploadedOn: Long = -1,
  var photosPerPage: Int = 0
) {

  fun getLastUploadedOn(): Long = lastUploadedOn

  fun updateFromReceivedPhotos(receivedPhotos: List<ReceivedPhoto>) {
    if (receivedPhotos.isEmpty()) {
      return
    }

    TODO()
//    lastUploadedOn = receivedPhotos.last().
  }


  fun reset() {
    lastUploadedOn = -1
  }
}