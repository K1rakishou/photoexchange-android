package com.kirakishou.photoexchange.mvp.model.photo

data class PhotoAdditionalInfo(
  val photoName: String,
  val isFavourited: Boolean,
  val favouritesCount: Long,
  val isReported: Boolean,
  val hasUserId: Boolean = false
) {

  companion object {
    fun create(
      photoName: String,
      isFavourited: Boolean,
      favouritesCount: Long,
      isReported: Boolean
    ): PhotoAdditionalInfo {
      return PhotoAdditionalInfo(photoName, isFavourited, favouritesCount, isReported)
    }

    fun empty(photoName: String): PhotoAdditionalInfo {
      return PhotoAdditionalInfo(photoName, false, 0L, false, false)
    }
  }
}