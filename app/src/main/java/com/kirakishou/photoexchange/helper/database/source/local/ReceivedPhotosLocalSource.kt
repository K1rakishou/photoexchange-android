package com.kirakishou.photoexchange.helper.database.source.local

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.ReceivedPhotoEntity
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.database.mapper.ReceivedPhotosMapper
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.PhotoExchangedData
import com.kirakishou.photoexchange.mvp.model.photo.ReceivedPhoto
import net.response.ReceivedPhotosResponse

class ReceivedPhotosLocalSource(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils,
  private val insertedEarlierThanTimeDelta: Long
) {
  private val TAG = "ReceivedPhotosLocalSource"
  private val receivedPhotosDao = database.receivedPhotoDao()

  fun save(receivedPhoto: ReceivedPhotosResponse.ReceivedPhotoResponseData): Boolean {
    val now = timeUtils.getTimeFast()

    return receivedPhotosDao.save(
      ReceivedPhotosMapper.FromObject.toReceivedPhotoEntity(now, receivedPhoto)
    ).isSuccess()
  }

  fun save(photoExchangedData: PhotoExchangedData): Boolean {
    val now = timeUtils.getTimeFast()

    val receivedPhotoEntity = ReceivedPhotoEntity(
      photoExchangedData.uploadedPhotoName,
      photoExchangedData.receivedPhotoName,
      photoExchangedData.lon,
      photoExchangedData.lat,
      photoExchangedData.uploadedOn,
      now
    )

    return receivedPhotosDao.save(receivedPhotoEntity).isSuccess()
  }

  fun saveMany(
    receivedPhotos: List<ReceivedPhotosResponse.ReceivedPhotoResponseData>
  ): Boolean {
    val time = timeUtils.getTimeFast()
    val photos = ReceivedPhotosMapper.FromResponse.GetReceivedPhotos
      .toReceivedPhotoEntities(time, receivedPhotos)

    return receivedPhotosDao.saveMany(photos).size == receivedPhotos.size
  }

  fun count(): Int {
    return receivedPhotosDao.countAll().toInt()
  }

  fun contains(uploadedPhotoName: String): Boolean {
    return receivedPhotosDao.findByUploadedPhotoName(uploadedPhotoName) != null
  }

  fun findMany(receivedPhotoIds: List<Long>): MutableList<ReceivedPhoto> {
    val photos = receivedPhotosDao.findMany(receivedPhotoIds)
    return ReceivedPhotosMapper.FromEntity.toReceivedPhotos(photos)
  }

  fun findOld(): List<ReceivedPhoto> {
    val now = timeUtils.getTimeFast() - insertedEarlierThanTimeDelta
    val photos =  receivedPhotosDao.findOld(now)

    return ReceivedPhotosMapper.FromEntity.toReceivedPhotos(photos)
  }

  fun getPage(lastUploadedOn: Long, count: Int): List<ReceivedPhoto> {
    val deletionTime = timeUtils.getTimeFast() - insertedEarlierThanTimeDelta
    val photos = receivedPhotosDao.getPage(lastUploadedOn, deletionTime, count)

    return ReceivedPhotosMapper.FromEntity.toReceivedPhotos(photos)
  }

  fun deleteAll() {
    receivedPhotosDao.deleteAll()
  }

  fun deleteByPhotoName(photoName: String) {
    receivedPhotosDao.deleteByPhotoName(photoName)
  }

}