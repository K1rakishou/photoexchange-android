package com.kirakishou.photoexchange.mvp.model.photo

import com.kirakishou.photoexchange.mvp.model.PhotoSize

data class GalleryPhoto(
  val photoName: String,
  val lon: Double,
  val lat: Double,
  val uploadedOn: Long,
  val favouritesCount: Long,
  val galleryPhotoInfo: GalleryPhotoInfo,
  val showPhoto: Boolean = true,
  val photoSize: PhotoSize = PhotoSize.Medium
)