package com.kirakishou.photoexchange.helper.database.source.local

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotosMapper
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvrx.model.photo.GalleryPhoto
import timber.log.Timber

open class GalleryPhotoLocalSource(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils,
  private val insertedEarlierThanTimeDelta: Long
) {
  private val TAG = "GalleryPhotoLocalSource"
  private val galleryPhotoDao = database.galleryPhotoDao()

  open fun save(galleryPhoto: GalleryPhoto): Boolean {
    val now = timeUtils.getTimeFast()
    val photo = GalleryPhotosMapper.FromObject.toGalleryPhotoEntity(now, galleryPhoto)

    return galleryPhotoDao.save(photo).isSuccess()
  }

  open fun saveMany(galleryPhotos: List<GalleryPhoto>): Boolean {
    val now = timeUtils.getTimeFast()
    val photos = GalleryPhotosMapper.FromObject.toGalleryPhotoEntities(now, galleryPhotos)

    return galleryPhotoDao.saveMany(photos).size == galleryPhotos.size
  }

  open fun getPage(lastUploadedOn: Long?, count: Int): List<GalleryPhoto> {
    val lastUploadedOnTime = lastUploadedOn ?: timeUtils.getTimePlus26Hours()
    val deletionTime = timeUtils.getTimeFast() - insertedEarlierThanTimeDelta

    val photos = galleryPhotoDao.getPage(lastUploadedOnTime, deletionTime, count)
    return GalleryPhotosMapper.FromEntity.toGalleryPhotos(photos)
  }

  open fun findByPhotoName(photoName: String): GalleryPhoto? {
    val galleryPhotoEntity = galleryPhotoDao.find(photoName)
    if (galleryPhotoEntity == null) {
      return null
    }

    return GalleryPhotosMapper.FromEntity.toGalleryPhoto(galleryPhotoEntity)
  }

  open fun findAll(): List<GalleryPhoto> {
    return GalleryPhotosMapper.FromEntity.toGalleryPhotos(galleryPhotoDao.findAll())
  }

  open fun deleteOldPhotos() {
    val now = timeUtils.getTimeFast()
    galleryPhotoDao.deleteOlderThan(now - insertedEarlierThanTimeDelta)

    Timber.tag(TAG).d("deleteOld called")
  }

  open fun deleteAll() {
    galleryPhotoDao.deleteAll()
  }

  open fun deleteByPhotoName(photoName: String) {
    galleryPhotoDao.deleteByPhotoName(photoName)
  }

}