package com.kirakishou.photoexchange.mvp.model.photo

data class GalleryPhotoInfo(
  val photoName: String,
  val isFavourited: Boolean,
  val isReported: Boolean,
  val type: Type = Type.Normal
) {

  enum class Type {
    NoUserId,
    Normal
  }

  companion object {
    fun empty(photoName: String): GalleryPhotoInfo {
      return GalleryPhotoInfo(photoName, false, false, Type.Normal)
    }
  }
}