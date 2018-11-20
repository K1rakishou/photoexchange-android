package com.kirakishou.photoexchange.helper.database.source.local

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.isFail
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.PhotoState

open class TakenPhotosLocalSource(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils
) {
  private val takenPhotoDao = database.takenPhotoDao()
  private val tempFilesDao = database.tempFileDao()

  open suspend fun updatePhotoState(photoId: Long, newPhotoState: PhotoState): Boolean {
    return takenPhotoDao.updateSetNewPhotoState(photoId, newPhotoState) == 1
  }

  suspend fun deletePhotoByName(photoName: String): Boolean {
    val photoId = takenPhotoDao.findPhotoIdByName(photoName)
    if (photoId == null) {
      //already deleted
      return true
    }

    return deletePhotoById(photoId)
  }

  open suspend fun deletePhotoById(photoId: Long): Boolean {
    if (takenPhotoDao.deleteById(photoId).isFail()) {
      return false
    }

    val tempFileEntity = tempFilesDao.findById(photoId)
    if (tempFileEntity == null) {
      //has already been deleted
      return true
    }

    val time = timeUtils.getTimeFast()
    if (tempFilesDao.markDeletedById(photoId, time).isFail()) {
      return false
    }

    return true
  }

}