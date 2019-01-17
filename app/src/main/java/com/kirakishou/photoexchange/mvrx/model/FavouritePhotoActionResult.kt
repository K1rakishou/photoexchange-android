package com.kirakishou.photoexchange.mvrx.model

data class FavouritePhotoActionResult(
  val photoName: String,
  val isFavourited: Boolean,
  val favouritesCount: Long
)