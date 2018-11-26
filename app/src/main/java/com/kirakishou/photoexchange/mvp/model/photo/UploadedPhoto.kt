package com.kirakishou.photoexchange.mvp.model.photo

import com.kirakishou.photoexchange.helper.PhotoSize

data class UploadedPhoto(
  val photoId: Long,
  val photoName: String,
  val uploaderLon: Double,
  val uploaderLat: Double,
  val receiverInfo: ReceiverInfo?,
  val uploadedOn: Long,
  val photoSize: PhotoSize = PhotoSize.Medium
) {

  fun isEmpty(): Boolean {
    return photoId == -1L
  }

  data class ReceiverInfo(
    val receiverLon: Double,
    val receiverLat: Double
  )

  companion object {
    fun empty(): UploadedPhoto {
      return UploadedPhoto(-1L, "", 0.0, 0.0, null, -1L, PhotoSize.Medium)
    }
  }
}