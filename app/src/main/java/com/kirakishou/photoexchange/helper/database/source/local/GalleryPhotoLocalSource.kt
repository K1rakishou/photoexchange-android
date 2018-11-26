package com.kirakishou.photoexchange.helper.database.source.local

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotosMapper
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhoto
import net.response.GalleryPhotosResponse

open class GalleryPhotoLocalSource(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils,
  private val galleryPhotoCacheMaxLiveTime: Long
) {
  private val TAG = "GalleryPhotoLocalSource"
  private val galleryPhotoDao = database.galleryPhotoDao()

  open fun saveMany(galleryPhotos: List<GalleryPhotosResponse.GalleryPhotoResponseData>): Boolean {
    val now = timeUtils.getTimeFast()
    return galleryPhotoDao.saveMany(GalleryPhotosMapper.FromResponse.ToEntity
      .toGalleryPhotoEntitiesList(now, galleryPhotos)).size == galleryPhotos.size
  }

  open fun getPage(time: Long, count: Int): List<GalleryPhoto> {
    return GalleryPhotosMapper.FromEntity.toGalleryPhotos(galleryPhotoDao.getPage(time, count))
  }

  open fun findByPhotoName(photoName: String): GalleryPhoto? {
    val galleryPhotoEntity = galleryPhotoDao.findByPhotoName(photoName)
    if (galleryPhotoEntity == null) {
      return null
    }

    return GalleryPhotosMapper.FromEntity.toGalleryPhoto(galleryPhotoEntity)
  }

  open fun findMany(galleryPhotoIds: List<Long>): List<GalleryPhoto> {
    return GalleryPhotosMapper.FromEntity.toGalleryPhotos(galleryPhotoDao.findMany(galleryPhotoIds))
  }

  open fun findAllPhotos(): List<GalleryPhoto> {
    return GalleryPhotosMapper.FromEntity.toGalleryPhotos(galleryPhotoDao.findAll())
  }

  fun deleteOld() {
    val now = timeUtils.getTimeFast()
    galleryPhotoDao.deleteOlderThan(now - galleryPhotoCacheMaxLiveTime)
  }
}