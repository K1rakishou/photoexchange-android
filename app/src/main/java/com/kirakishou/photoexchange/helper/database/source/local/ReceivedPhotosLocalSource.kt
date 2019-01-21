package com.kirakishou.photoexchange.helper.database.source.local

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.ReceivedPhotoEntity
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.database.mapper.ReceivedPhotosMapper
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvrx.model.NewReceivedPhoto
import com.kirakishou.photoexchange.mvrx.model.photo.ReceivedPhoto
import net.response.data.ReceivedPhotoResponseData

class ReceivedPhotosLocalSource(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils,
  private val insertedEarlierThanTimeDelta: Long
) {
  private val TAG = "ReceivedPhotosLocalSource"
  private val receivedPhotosDao = database.receivedPhotoDao()

  fun save(receivedPhoto: ReceivedPhotoResponseData): Boolean {
    val now = timeUtils.getTimeFast()

    return receivedPhotosDao.save(
      ReceivedPhotosMapper.FromResponse.ReceivedPhotos.toReceivedPhotoEntity(now, receivedPhoto)
    ).isSuccess()
  }

  fun save(newReceivedPhoto: NewReceivedPhoto): Boolean {
    val now = timeUtils.getTimeFast()

    val receivedPhotoEntity = ReceivedPhotoEntity(
      newReceivedPhoto.uploadedPhotoName,
      newReceivedPhoto.receivedPhotoName,
      newReceivedPhoto.lon,
      newReceivedPhoto.lat,
      newReceivedPhoto.uploadedOn,
      now
    )

    return receivedPhotosDao.save(receivedPhotoEntity).isSuccess()
  }

  fun save(receivedPhoto: ReceivedPhoto): Boolean {
    val time = timeUtils.getTimeFast()
    val photo = ReceivedPhotosMapper.FromObject.toReceivedPhotoEntity(time, receivedPhoto)
    return receivedPhotosDao.save(photo).isSuccess()
  }

  fun saveMany(receivedPhotos: List<ReceivedPhoto>): Boolean {
    val time = timeUtils.getTimeFast()
    val photos = ReceivedPhotosMapper.FromObject.toReceivedPhotoEntities(time, receivedPhotos)
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

  fun getPage(lastUploadedOn: Long?, count: Int): List<ReceivedPhoto> {
    val lastUploadedOnTime = lastUploadedOn ?: timeUtils.getTimePlus26Hours()
    val deletionTime = timeUtils.getTimeFast() - insertedEarlierThanTimeDelta

    val photos = receivedPhotosDao.getPage(lastUploadedOnTime, deletionTime, count)
    return ReceivedPhotosMapper.FromEntity.toReceivedPhotos(photos)
  }

  fun deleteAll() {
    receivedPhotosDao.deleteAll()
  }

  fun deleteByPhotoName(photoName: String) {
    receivedPhotosDao.deleteByPhotoName(photoName)
  }

}