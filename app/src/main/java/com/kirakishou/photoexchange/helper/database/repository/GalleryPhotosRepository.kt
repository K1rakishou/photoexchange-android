package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.source.local.GalleryPhotoLocalSource
import com.kirakishou.photoexchange.mvrx.model.photo.GalleryPhoto

class GalleryPhotosRepository(
  private val galleryPhotoLocalSource: GalleryPhotoLocalSource
) : BaseRepository() {

  suspend fun saveMany(galleryPhotos: List<GalleryPhoto>): Boolean {
    return galleryPhotoLocalSource.saveMany(galleryPhotos)
  }

  suspend fun findPhotoByPhotoName(photoName: String): GalleryPhoto? {
    return galleryPhotoLocalSource.findByPhotoName(photoName)
  }

  suspend fun getPage(lastUploadedOn: Long?, count: Int): List<GalleryPhoto> {
    return galleryPhotoLocalSource.getPage(lastUploadedOn, count)
  }

  suspend fun deleteOldPhotos() {
    galleryPhotoLocalSource.deleteOldPhotos()
  }

  suspend fun deleteAll() {
    galleryPhotoLocalSource.deleteAll()
  }

  suspend fun deleteByPhotoName(photoName: String) {
    galleryPhotoLocalSource.deleteByPhotoName(photoName)
  }

}