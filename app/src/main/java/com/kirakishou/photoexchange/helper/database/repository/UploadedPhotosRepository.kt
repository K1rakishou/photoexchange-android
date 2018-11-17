package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.UploadedPhotoEntity
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.database.mapper.UploadedPhotosMapper
import com.kirakishou.photoexchange.helper.extension.minutes
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.net.response.GetUploadedPhotosResponse
import timber.log.Timber

open class UploadedPhotosRepository(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils,
  private val uploadedPhotoMaxCacheLiveTime: Long
) {
  private val TAG = "UploadedPhotosRepository"
  private val uploadedPhotoDao = database.uploadedPhotoDao()

  open fun save(photoId: Long, photoName: String, lon: Double, lat: Double, uploadedOn: Long): Boolean {
    val uploadedPhotoEntity = UploadedPhotosMapper.FromObject.ToEntity.toUploadedPhotoEntity(photoId, photoName, lon,
      lat, timeUtils.getTimeFast(), uploadedOn)
    return uploadedPhotoDao.save(uploadedPhotoEntity).isSuccess()
  }

  open fun saveMany(uploadedPhotoDataList: List<GetUploadedPhotosResponse.UploadedPhotoData>): Boolean {
    return database.transactional {
      val now = timeUtils.getTimeFast()

      for (uploadedPhotoData in uploadedPhotoDataList) {
        val photo = UploadedPhotosMapper.FromResponse.ToEntity.toUploadedPhotoEntity(now, uploadedPhotoData)
        if (!save(photo)) {
          return@transactional false
        }
      }

      return@transactional true
    }
  }

  private fun save(uploadedPhotoEntity: UploadedPhotoEntity): Boolean {
    val cachedPhoto = uploadedPhotoDao.findByPhotoName(uploadedPhotoEntity.photoName)
    if (cachedPhoto != null) {
      uploadedPhotoEntity.photoId = cachedPhoto.photoId
    }

    return uploadedPhotoDao.save(uploadedPhotoEntity).isSuccess()
  }

  fun getPageOfUploadedPhotos(time: Long, count: Int): List<UploadedPhoto> {
    return UploadedPhotosMapper.FromEntity.ToObject.toUploadedPhotos(uploadedPhotoDao.getPage(time, count))
  }

  fun count(): Int {
    return uploadedPhotoDao.count().toInt()
  }

  open fun findMany(photoIds: List<Long>): List<UploadedPhoto> {
    return UploadedPhotosMapper.FromEntity.ToObject.toUploadedPhotos(uploadedPhotoDao.findMany(photoIds))
  }

  fun findAll(): List<UploadedPhoto> {
    return UploadedPhotosMapper.FromEntity.ToObject.toUploadedPhotos(uploadedPhotoDao.findAll())
  }

  fun findAllTest(): List<UploadedPhotoEntity> {
    return uploadedPhotoDao.findAll()
  }

  fun findAllWithReceiverInfo(): List<UploadedPhoto> {
    val entities = uploadedPhotoDao.findAllWithReceiverInfo()
    return UploadedPhotosMapper.FromEntity.ToObject.toUploadedPhotos(entities)
  }

  open fun findAllWithoutReceiverInfo(): List<UploadedPhoto> {
    val entities = uploadedPhotoDao.findAllWithoutReceiverInfo()
    return UploadedPhotosMapper.FromEntity.ToObject.toUploadedPhotos(entities)
  }

  fun updateReceiverInfo(uploadedPhotoName: String): Boolean {
    return uploadedPhotoDao.updateReceiverInfo(uploadedPhotoName) == 1
  }

  open fun deleteOld() {
    val oldCount = findAllTest().size
    val now = timeUtils.getTimeFast()
    uploadedPhotoDao.deleteOlderThan(now - uploadedPhotoMaxCacheLiveTime)

    val newCount = findAllTest().size
    if (newCount < oldCount) {
      Timber.tag(TAG).d("Deleted ${newCount - oldCount} uploadedPhotos from the cache")
    }
  }

  fun deleteAll() {
    uploadedPhotoDao.deleteAll()
  }
}