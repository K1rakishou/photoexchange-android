package com.kirakishou.photoexchange.mvrx.model.photo

import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.mvrx.model.PhotoSize

data class GalleryPhoto(
  val photoName: String,
  val lonLat: LonLat,
  val uploadedOn: Long,
  val photoAdditionalInfo: PhotoAdditionalInfo,
  val showPhoto: Boolean = true,
  val photoSize: PhotoSize = PhotoSize.Medium
)