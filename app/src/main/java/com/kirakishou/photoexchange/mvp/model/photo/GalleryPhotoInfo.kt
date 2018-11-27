package com.kirakishou.photoexchange.mvp.model.photo

data class GalleryPhotoInfo(
  val photoName: String,
  val isFavourited: Boolean,
  val isReported: Boolean
) {

  fun isEmpty(): Boolean {
    return photoName == ""
  }

  companion object {
    fun empty(): GalleryPhotoInfo {
      return GalleryPhotoInfo("", false, false)
    }
  }
}