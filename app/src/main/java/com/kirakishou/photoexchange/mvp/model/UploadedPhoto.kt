package com.kirakishou.photoexchange.mvp.model

data class UploadedPhoto(
  var photoId: Long,
  val photoName: String,
  val uploaderLon: Double,
  val uploaderLat: Double,
  var hasReceiverInfo: Boolean,
  val uploadedOn: Long
) {

  fun isEmpty(): Boolean {
    return photoId == -1L
  }

  companion object {
    fun empty(): UploadedPhoto {
      return UploadedPhoto(-1L, "", 0.0, 0.0, false, -1L)
    }
  }
}