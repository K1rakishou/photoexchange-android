package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.GalleryPhotoInfoEntity
import com.kirakishou.photoexchange.helper.database.source.local.GalleryPhotoInfoLocalSource
import com.kirakishou.photoexchange.helper.database.source.local.GalleryPhotoLocalSource
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhotoInfo
import net.response.GalleryPhotoInfoResponse
import net.response.GalleryPhotosResponse

class GalleryPhotosRepository(
  private val database: MyDatabase,
  private val galleryPhotoLocalSource: GalleryPhotoLocalSource,
  private val galleryPhotoInfoLocalSource: GalleryPhotoInfoLocalSource
) : BaseRepository() {

  suspend fun findPhotoByPhotoName(photoName: String): GalleryPhoto? {
    return galleryPhotoLocalSource.findByPhotoName(photoName)
  }

  suspend fun findPhotoInfoByPhotoName(photoName: String): GalleryPhotoInfoEntity? {
    return galleryPhotoInfoLocalSource.find(photoName)
  }

  suspend fun findMany(photoNameList: List<String>): List<GalleryPhotoInfo> {
    return galleryPhotoInfoLocalSource.findMany(photoNameList)
  }

  suspend fun save(galleryPhotoInfoEntity: GalleryPhotoInfoEntity): Boolean {
    return galleryPhotoInfoLocalSource.save(galleryPhotoInfoEntity)
  }

  suspend fun saveMany(galleryPhotos: List<GalleryPhotosResponse.GalleryPhotoResponseData>): Boolean {
    return galleryPhotoLocalSource.saveMany(galleryPhotos)
  }

  suspend fun saveManyPhotoInfo(
    galleryPhotoInfoList: List<GalleryPhotoInfoResponse.GalleryPhotosInfoResponseData>
  ): Boolean {
    return galleryPhotoInfoLocalSource.saveMany(galleryPhotoInfoList)
  }

  suspend fun getPage(time: Long, count: Int): List<GalleryPhoto> {
    return galleryPhotoLocalSource.getPage(time, count)
  }

  suspend fun deleteOldPhotos() {
    database.transactional {
      galleryPhotoLocalSource.deleteOldPhotos()
      galleryPhotoInfoLocalSource.deleteOldPhotoInfos()
    }
  }

  suspend fun deleteAll() {
    database.transactional {
      galleryPhotoLocalSource.deleteAll()
      galleryPhotoInfoLocalSource.deleteAll()
    }
  }

  suspend fun deleteByPhotoName(photoName: String) {
    database.transactional {
      galleryPhotoLocalSource.deleteByPhotoName(photoName)
      galleryPhotoInfoLocalSource.deleteByPhotoName(photoName)
    }
  }
}