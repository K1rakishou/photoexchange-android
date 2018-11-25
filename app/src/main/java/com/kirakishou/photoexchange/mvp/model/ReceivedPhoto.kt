package com.kirakishou.photoexchange.mvp.model

data class ReceivedPhoto(
  val photoId: Long,
  val uploadedPhotoName: String,
  val receivedPhotoName: String,
  val lon: Double,
  val lat: Double,
  val uploadedOn: Long
)