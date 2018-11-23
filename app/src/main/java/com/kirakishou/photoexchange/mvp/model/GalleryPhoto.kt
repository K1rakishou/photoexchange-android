package com.kirakishou.photoexchange.mvp.model

import com.kirakishou.photoexchange.mvp.model.GalleryPhotoInfo

data class GalleryPhoto(
  val galleryPhotoId: Long,
  val photoName: String,
  val lon: Double,
  val lat: Double,
  val uploadedOn: Long,
  var favouritesCount: Long,
  var galleryPhotoInfo: GalleryPhotoInfo? = null
)