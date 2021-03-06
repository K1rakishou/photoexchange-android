package com.kirakishou.photoexchange.mvrx.model.photo

import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.mvrx.model.PhotoSize

data class ReceivedPhoto(
  val uploadedPhotoName: String,
  val receivedPhotoName: String,
  val lonLat: LonLat,
  val uploadedOn: Long,
  val photoAdditionalInfo: PhotoAdditionalInfo? = null,
  val showPhoto: Boolean = true,
  val photoSize: PhotoSize = PhotoSize.Medium
)