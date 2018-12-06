package com.kirakishou.photoexchange.helper.database.source.local

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.UploadedPhotoEntity
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.database.mapper.UploadedPhotosMapper
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.photo.UploadedPhoto
import net.response.GetUploadedPhotosResponse

open class UploadPhotosLocalSource(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils
) {
  private val uploadedPhotoDao = database.uploadedPhotoDao()

  open fun save(photoId: Long, photoName: String, lon: Double, lat: Double, uploadedOn: Long): Boolean {
    val uploadedPhotoEntity = UploadedPhotosMapper.FromObject.ToEntity.toUploadedPhotoEntity(
      photoId,
      photoName,
      lon,
      lat,
      timeUtils.getTimeFast(),
      uploadedOn
    )
    return uploadedPhotoDao.save(uploadedPhotoEntity).isSuccess()
  }

  private fun save(uploadedPhotoEntity: UploadedPhotoEntity): Boolean {
    val cachedPhoto = uploadedPhotoDao.findByPhotoName(uploadedPhotoEntity.photoName)
    if (cachedPhoto != null) {
      uploadedPhotoEntity.photoId = cachedPhoto.photoId
    }

    return uploadedPhotoDao.save(uploadedPhotoEntity).isSuccess()
  }

  open fun saveMany(
    uploadedPhotoDataList: List<GetUploadedPhotosResponse.UploadedPhotoResponseData>
  ): Boolean {
    val now = timeUtils.getTimeFast()

    for (uploadedPhotoData in uploadedPhotoDataList) {
      val photo = UploadedPhotosMapper.FromResponse.ToEntity.toUploadedPhotoEntity(now, uploadedPhotoData)
      if (!save(photo)) {
        return false
      }
    }

    return true
  }

  fun getPage(time: Long, count: Int): List<UploadedPhoto> {
    return UploadedPhotosMapper.FromEntity.ToObject.toUploadedPhotos(uploadedPhotoDao.getPage(time, count))
  }

  fun updateReceiverInfo(
    uploadedPhotoName: String,
    receivedPhotoName: String,
    receiverLon: Double,
    receiverLat: Double
  ): Boolean {
    return uploadedPhotoDao.updateReceiverInfo(
      uploadedPhotoName,
      receivedPhotoName,
      receiverLon,
      receiverLat
    ) == 1
  }

  fun deleteAll() {
    uploadedPhotoDao.deleteAll()
  }
}