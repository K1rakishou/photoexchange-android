package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.UploadedPhotoEntity
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.database.mapper.UploadedPhotosMapper
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import kotlinx.coroutines.withContext
import net.response.GetUploadedPhotosResponse
import timber.log.Timber

open class UploadedPhotosRepository(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils,
  private val uploadedPhotoMaxCacheLiveTime: Long,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {
  private val TAG = "UploadedPhotosRepository"
  private val uploadedPhotoDao = database.uploadedPhotoDao()

  open suspend fun save(photoId: Long, photoName: String, lon: Double, lat: Double, uploadedOn: Long): Boolean {
    return withContext(coroutineContext) {
      val uploadedPhotoEntity = UploadedPhotosMapper.FromObject.ToEntity.toUploadedPhotoEntity(
        photoId,
        photoName,
        lon,
        lat,
        timeUtils.getTimeFast(),
        uploadedOn
      )
      return@withContext uploadedPhotoDao.save(uploadedPhotoEntity).isSuccess()
    }
  }

  open suspend fun saveMany(uploadedPhotoDataList: List<GetUploadedPhotosResponse.UploadedPhotoResponseData>): Boolean {
    return withContext(coroutineContext) {
      return@withContext database.transactional {
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
  }

  private suspend fun save(uploadedPhotoEntity: UploadedPhotoEntity): Boolean {
    val cachedPhoto = uploadedPhotoDao.findByPhotoName(uploadedPhotoEntity.photoName)
    if (cachedPhoto != null) {
      uploadedPhotoEntity.photoId = cachedPhoto.photoId
    }

    return uploadedPhotoDao.save(uploadedPhotoEntity).isSuccess()
  }

  suspend fun getPageOfUploadedPhotos(time: Long, count: Int): List<UploadedPhoto> {
    return withContext(coroutineContext) {
      return@withContext UploadedPhotosMapper.FromEntity.ToObject.toUploadedPhotos(uploadedPhotoDao.getPage(time, count))
    }
  }

  suspend fun count(): Int {
    return withContext(coroutineContext) {
      return@withContext uploadedPhotoDao.count().toInt()
    }
  }

  open suspend fun findMany(photoIds: List<Long>): List<UploadedPhoto> {
    return withContext(coroutineContext) {
      return@withContext UploadedPhotosMapper.FromEntity.ToObject.toUploadedPhotos(uploadedPhotoDao.findMany(photoIds))
    }
  }

  suspend fun findAll(): List<UploadedPhoto> {
    return withContext(coroutineContext) {
      return@withContext UploadedPhotosMapper.FromEntity.ToObject.toUploadedPhotos(uploadedPhotoDao.findAll())
    }
  }

  suspend fun findAllTest(): List<UploadedPhotoEntity> {
    return withContext(coroutineContext) {
      return@withContext uploadedPhotoDao.findAll()
    }
  }

  suspend fun findAllWithReceiverInfo(): List<UploadedPhoto> {
    return withContext(coroutineContext) {
      val entities = uploadedPhotoDao.findAllWithReceiverInfo()
      return@withContext UploadedPhotosMapper.FromEntity.ToObject.toUploadedPhotos(entities)
    }
  }

  open suspend fun findAllWithoutReceiverInfo(): List<UploadedPhoto> {
    return withContext(coroutineContext) {
      val entities = uploadedPhotoDao.findAllWithoutReceiverInfo()
      return@withContext UploadedPhotosMapper.FromEntity.ToObject.toUploadedPhotos(entities)
    }
  }

  suspend fun updateReceiverInfo(uploadedPhotoName: String): Boolean {
    return withContext(coroutineContext) {
      return@withContext uploadedPhotoDao.updateReceiverInfo(uploadedPhotoName) == 1
    }
  }

  //TODO: tests
  open suspend fun deleteOldPhotos() {
    withContext(coroutineContext) {
      val oldCount = findAllTest().size
      val now = timeUtils.getTimeFast()
      uploadedPhotoDao.deleteOlderThan(now - uploadedPhotoMaxCacheLiveTime)

      val newCount = findAllTest().size
      if (newCount < oldCount) {
        Timber.tag(TAG).d("Deleted ${newCount - oldCount} uploadedPhotos from the cache")
      }
    }
  }

  suspend fun deleteAll() {
    withContext(coroutineContext) {
      uploadedPhotoDao.deleteAll()
    }
  }
}