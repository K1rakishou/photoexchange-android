package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.source.local.ReceivedPhotosLocalSource
import com.kirakishou.photoexchange.mvp.model.photo.ReceivedPhoto
import kotlinx.coroutines.withContext
import net.response.ReceivedPhotosResponse

open class ReceivedPhotosRepository(
  private val database: MyDatabase,
  private val receivedPhotosLocalSource: ReceivedPhotosLocalSource,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {
  private val TAG = "ReceivedPhotosRepository"

  suspend fun save(receivedPhoto: ReceivedPhotosResponse.ReceivedPhotoResponseData): Boolean {
    return withContext(coroutineContext) {
      return@withContext receivedPhotosLocalSource.save(receivedPhoto)
    }
  }

  suspend fun saveMany(receivedPhotos: List<ReceivedPhotosResponse.ReceivedPhotoResponseData>): Boolean {
    return withContext(coroutineContext) {
      return@withContext receivedPhotosLocalSource.saveMany(receivedPhotos)
    }
  }

  suspend fun count(): Int {
    return withContext(coroutineContext) {
      return@withContext receivedPhotosLocalSource.count()
    }
  }

  suspend fun contains(uploadedPhotoName: String): Boolean {
    return withContext(coroutineContext) {
      return@withContext receivedPhotosLocalSource.contains(uploadedPhotoName)
    }
  }

  suspend fun findMany(receivedPhotoIds: List<Long>): MutableList<ReceivedPhoto> {
    return withContext(coroutineContext) {
      return@withContext receivedPhotosLocalSource.findMany(receivedPhotoIds)
    }
  }

  suspend fun deleteOldPhotos() {
    withContext(coroutineContext) {
      receivedPhotosLocalSource.deleteOldPhotos()
    }
  }

  suspend fun deleteAll() {
    withContext(coroutineContext) {
      receivedPhotosLocalSource.deleteAll()
    }
  }
}