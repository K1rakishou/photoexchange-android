package com.kirakishou.photoexchange.mvp.model

data class FavouritePhotoActionResult(
  val photoName: String,
  val isFavourited: Boolean,
  val favouritesCount: Long
)