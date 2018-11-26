package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.UploadedPhotoEntity
import com.kirakishou.photoexchange.helper.database.mapper.UploadedPhotosMapper
import com.kirakishou.photoexchange.mvp.model.photo.UploadedPhoto
import kotlinx.coroutines.withContext

open class UploadedPhotosRepository(
  private val database: MyDatabase,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {
  private val TAG = "UploadedPhotosRepository"
  private val uploadedPhotoDao = database.uploadedPhotoDao()

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

  suspend fun deleteAll() {
    withContext(coroutineContext) {
      uploadedPhotoDao.deleteAll()
    }
  }
}