package com.kirakishou.photoexchange.helper.database.source.local

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.database.mapper.UploadedPhotosMapper
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.PhotoState

open class UploadPhotosLocalSource(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils
) {
  private val uploadedPhotoDao = database.uploadedPhotoDao()
  private val takenPhotoDao = database.takenPhotoDao()

  open suspend fun save(photoId: Long, photoName: String, lon: Double, lat: Double, uploadedOn: Long): Boolean {
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

  open suspend fun updatePhotoState(photoId: Long, newPhotoState: PhotoState): Boolean {
    return takenPhotoDao.updateSetNewPhotoState(photoId, newPhotoState) == 1
  }
}