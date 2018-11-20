package com.kirakishou.photoexchange.helper.database.source.local

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.ReceivedPhotoEntity
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.database.mapper.ReceivedPhotosMapper
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import net.response.ReceivedPhotosResponse

class ReceivePhotosLocalSource(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils,
  private val receivedPhotoMaxCacheLiveTime: Long
) {
  private val receivedPhotosDao = database.receivedPhotoDao()

  fun save(receivedPhoto: ReceivedPhotosResponse.ReceivedPhotoResponseData): Boolean {
    val now = timeUtils.getTimeFast()

    return receivedPhotosDao.save(
      ReceivedPhotosMapper.FromObject.toReceivedPhotoEntity(now, receivedPhoto)
    ).isSuccess()
  }

  fun saveMany(receivedPhotos: List<ReceivedPhotosResponse.ReceivedPhotoResponseData>): Boolean {
    val time = timeUtils.getTimeFast()
    return receivedPhotosDao.saveMany(ReceivedPhotosMapper.FromResponse.GetReceivedPhotos.toReceivedPhotoEntities(time, receivedPhotos))
      .size == receivedPhotos.size
  }

  fun getPageOfReceivedPhotos(lastUploadedOn: Long, count: Int): List<ReceivedPhoto> {
    return ReceivedPhotosMapper.FromEntity.toReceivedPhotos(receivedPhotosDao.getPage(lastUploadedOn, count))
  }

  fun findAll(): List<ReceivedPhotoEntity> {
      return receivedPhotosDao.findAll()
  }

  //TODO: tests
  fun deleteOldPhotos() {
    val now = timeUtils.getTimeFast()
    receivedPhotosDao.deleteOlderThan(now - receivedPhotoMaxCacheLiveTime)
  }

}