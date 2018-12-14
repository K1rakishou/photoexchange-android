package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.source.local.GalleryPhotoLocalSource
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhoto
import net.response.data.GalleryPhotoResponseData

class GalleryPhotosRepository(
  private val galleryPhotoLocalSource: GalleryPhotoLocalSource
) : BaseRepository() {

  suspend fun saveMany(galleryPhotos: List<GalleryPhotoResponseData>): Boolean {
    return galleryPhotoLocalSource.saveMany(galleryPhotos)
  }

  suspend fun findPhotoByPhotoName(photoName: String): GalleryPhoto? {
    return galleryPhotoLocalSource.findByPhotoName(photoName)
  }

  suspend fun getPage(time: Long, count: Int): List<GalleryPhoto> {
    return galleryPhotoLocalSource.getPage(time, count)
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