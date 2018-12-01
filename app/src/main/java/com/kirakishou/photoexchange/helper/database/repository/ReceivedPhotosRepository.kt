package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.Constants
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.ReceivedPhotoEntity
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.database.mapper.ReceivedPhotosMapper
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.PhotoExchangedData
import com.kirakishou.photoexchange.mvp.model.photo.ReceivedPhoto
import kotlinx.coroutines.withContext
import net.response.ReceivedPhotosResponse

open class ReceivedPhotosRepository(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {
  private val TAG = "ReceivedPhotosRepository"
  private val receivedPhotosDao = database.receivedPhotoDao()

  suspend fun save(receivedPhoto: ReceivedPhotosResponse.ReceivedPhotoResponseData): Boolean {
    return withContext(coroutineContext) {
      val now = timeUtils.getTimeFast()
      return@withContext receivedPhotosDao.save(ReceivedPhotosMapper.FromResponse.ReceivedPhotos
        .toReceivedPhotoEntity(now, receivedPhoto))
        .isSuccess()
    }
  }

  suspend fun count(): Int {
    return withContext(coroutineContext) {
      return@withContext receivedPhotosDao.countAll().toInt()
    }
  }

  suspend fun findMany(receivedPhotoIds: List<Long>): MutableList<ReceivedPhoto> {
    return withContext(coroutineContext) {
      return@withContext ReceivedPhotosMapper.FromEntity
        .toReceivedPhotos(receivedPhotosDao.findMany(receivedPhotoIds))
    }
  }

  suspend fun deleteAll() {
    withContext(coroutineContext) {
      receivedPhotosDao.deleteAll()
    }
  }

  fun deleteOldPhotos() {
    val now = timeUtils.getTimeFast()
    receivedPhotosDao.deleteOlderThan(now - Constants.RECEIVED_PHOTOS_CACHE_MAX_LIVE_TIME)
  }
}