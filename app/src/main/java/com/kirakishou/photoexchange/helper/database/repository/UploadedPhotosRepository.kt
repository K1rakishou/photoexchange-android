package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.source.local.UploadPhotosLocalSource
import com.kirakishou.photoexchange.mvp.model.photo.UploadedPhoto
import kotlinx.coroutines.withContext

open class UploadedPhotosRepository(
  private val database: MyDatabase,
  private val uploadedPhotosLocalSource: UploadPhotosLocalSource,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {
  private val TAG = "UploadedPhotosRepository"

  suspend fun count(): Int {
    return withContext(coroutineContext) {
      return@withContext uploadedPhotosLocalSource.count()
    }
  }

  suspend fun contains(photoName: String): Boolean {
    return withContext(coroutineContext) {
      return@withContext uploadedPhotosLocalSource.contains(photoName)
    }
  }

  open suspend fun findMany(photoIds: List<Long>): List<UploadedPhoto> {
    return withContext(coroutineContext) {
      return@withContext uploadedPhotosLocalSource.findMany(photoIds)
    }
  }

  suspend fun findAll(): List<UploadedPhoto> {
    return withContext(coroutineContext) {
      return@withContext uploadedPhotosLocalSource.findAll()
    }
  }

  suspend fun findAllWithReceiverInfo(): List<UploadedPhoto> {
    return withContext(coroutineContext) {
      return@withContext uploadedPhotosLocalSource.findAllWithReceiverInfo()
    }
  }

  open suspend fun findAllWithoutReceiverInfo(): List<UploadedPhoto> {
    return withContext(coroutineContext) {
      return@withContext uploadedPhotosLocalSource.findAllWithoutReceiverInfo()
    }
  }

  suspend fun deleteOldPhotos() {
    return uploadedPhotosLocalSource.deleteOldPhotos()
  }

  suspend fun deleteAll() {
    withContext(coroutineContext) {
      uploadedPhotosLocalSource.deleteAll()
    }
  }
}