package com.kirakishou.photoexchange.mvp.model.photo

import com.kirakishou.photoexchange.mvp.model.PhotoSize

data class ReceivedPhoto(
  val uploadedPhotoName: String,
  val receivedPhotoName: String,
  val lon: Double,
  val lat: Double,
  val uploadedOn: Long,
  val showPhoto: Boolean = true,
  val photoSize: PhotoSize = PhotoSize.Medium
)