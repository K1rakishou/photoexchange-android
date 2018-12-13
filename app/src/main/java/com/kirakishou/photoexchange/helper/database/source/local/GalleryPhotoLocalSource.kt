package com.kirakishou.photoexchange.helper.database.source.local

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotosMapper
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhoto
import net.response.GalleryPhotosResponse
import timber.log.Timber

open class GalleryPhotoLocalSource(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils,
  private val insertedEarlierThanTimeDelta: Long
) {
  private val TAG = "GalleryPhotoLocalSource"
  private val galleryPhotoDao = database.galleryPhotoDao()

  open fun saveMany(galleryPhotos: List<GalleryPhotosResponse.GalleryPhotoResponseData>): Boolean {
    val now = timeUtils.getTimeFast()
    return galleryPhotoDao.saveMany(GalleryPhotosMapper.FromResponse.ToEntity
      .toGalleryPhotoEntitiesList(now, galleryPhotos)).size == galleryPhotos.size
  }

  open fun getPage(time: Long, count: Int): List<GalleryPhoto> {
    val deletionTime = timeUtils.getTimeFast() - insertedEarlierThanTimeDelta
    val photos =galleryPhotoDao.getPage(time, deletionTime, count)

    return GalleryPhotosMapper.FromEntity.toGalleryPhotos(photos)
  }

  open fun findByPhotoName(photoName: String): GalleryPhoto? {
    val galleryPhotoEntity = galleryPhotoDao.find(photoName)
    if (galleryPhotoEntity == null) {
      return null
    }

    return GalleryPhotosMapper.FromEntity.toGalleryPhoto(galleryPhotoEntity)
  }

  open fun deleteOldPhotos() {
    val now = timeUtils.getTimeFast()
    val deletedCount = galleryPhotoDao.deleteOlderThan(now - insertedEarlierThanTimeDelta)

    Timber.tag(TAG).d("deleted $deletedCount gallery photos")
  }

  open fun deleteAll() {
    galleryPhotoDao.deleteAll()
  }

  fun deleteByPhotoName(photoName: String) {
    galleryPhotoDao.deleteByPhotoName(photoName)
  }
}