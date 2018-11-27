package com.kirakishou.photoexchange.mvp.model.photo

data class GalleryPhotoInfo(
  val galleryPhotoId: Long,
  val isFavourited: Boolean,
  val isReported: Boolean
) {

  fun isEmpty(): Boolean {
    return this.galleryPhotoId == -1L
  }

  companion object {
    fun empty(): GalleryPhotoInfo {
      return GalleryPhotoInfo(-1L, false, false)
    }
  }
}