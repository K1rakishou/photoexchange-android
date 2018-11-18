package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.ReceivedPhotoEntity
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.database.mapper.ReceivedPhotosMapper
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.net.response.GetReceivedPhotosResponse
import com.kirakishou.photoexchange.mvp.model.net.response.ReceivedPhotosResponse
import kotlinx.coroutines.withContext
import timber.log.Timber

open class ReceivedPhotosRepository(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils,
  private val receivedPhotoMaxCacheLiveTime: Long,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {
  private val TAG = "ReceivedPhotosRepository"
  private val receivedPhotosDao = database.receivedPhotoDao()

  suspend fun save(receivedPhoto: GetReceivedPhotosResponse.ReceivedPhoto): Long {
    return withContext(coroutineContext) {
      val now = timeUtils.getTimeFast()
      return@withContext receivedPhotosDao.save(ReceivedPhotosMapper.FromObject.toReceivedPhotoEntity(now, receivedPhoto))
    }
  }

  suspend fun save(receivedPhoto: ReceivedPhotosResponse.ReceivedPhoto): Boolean {
    return withContext(coroutineContext) {
      val now = timeUtils.getTimeFast()
      return@withContext receivedPhotosDao.save(ReceivedPhotosMapper.FromResponse.ReceivedPhotos.toReceivedPhotoEntity(now, receivedPhoto))
        .isSuccess()
    }
  }

  suspend fun saveMany(receivedPhotos: List<GetReceivedPhotosResponse.ReceivedPhoto>): Boolean {
    return withContext(coroutineContext) {
      val time = timeUtils.getTimeFast()
      return@withContext receivedPhotosDao.saveMany(ReceivedPhotosMapper.FromResponse.GetReceivedPhotos.toReceivedPhotoEntities(time, receivedPhotos))
        .size == receivedPhotos.size
    }
  }

  suspend fun getPageOfReceivedPhotos(time: Long, count: Int): List<ReceivedPhoto> {
    return withContext(coroutineContext) {
      return@withContext ReceivedPhotosMapper.FromEntity.toReceivedPhotos(receivedPhotosDao.getPage(time, count))
    }
  }

  suspend fun count(): Int {
    return withContext(coroutineContext) {
      return@withContext receivedPhotosDao.countAll().toInt()
    }
  }

  suspend fun findAllTest(): List<ReceivedPhotoEntity> {
    return withContext(coroutineContext) {
      return@withContext receivedPhotosDao.findAll()
    }
  }

  suspend fun findMany(receivedPhotoIds: List<Long>): MutableList<ReceivedPhoto> {
    return withContext(coroutineContext) {
      return@withContext ReceivedPhotosMapper.FromEntity.toReceivedPhotos(receivedPhotosDao.findMany(receivedPhotoIds))
    }
  }

  suspend fun deleteOld() {
    withContext(coroutineContext) {
      val oldCount = findAllTest().size
      val now = timeUtils.getTimeFast()
      receivedPhotosDao.deleteOlderThan(now - receivedPhotoMaxCacheLiveTime)

      val newCount = findAllTest().size
      if (newCount < oldCount) {
        Timber.tag(TAG).d("Deleted ${newCount - oldCount} receivedPhotos from the cache")
      }
    }
  }

  suspend fun deleteAll() {
    withContext(coroutineContext) {
      receivedPhotosDao.deleteAll()
    }
  }
}