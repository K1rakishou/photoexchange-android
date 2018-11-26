package com.kirakishou.photoexchange.mvp.model.photo

class GalleryPhotoInfo(
  val galleryPhotoId: Long,
  var isFavourited: Boolean,
  var isReported: Boolean
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