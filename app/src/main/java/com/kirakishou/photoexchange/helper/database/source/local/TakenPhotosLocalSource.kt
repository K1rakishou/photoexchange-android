package com.kirakishou.photoexchange.helper.database.source.local

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.isFail
import com.kirakishou.photoexchange.helper.util.TimeUtils

class TakenPhotosLocalSource(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils
  ) {
  private val takenPhotoDao = database.takenPhotoDao()
  private val tempFilesDao = database.tempFileDao()

  open suspend fun deletePhotoById(photoId: Long): Boolean {
    if (takenPhotoDao.deleteById(photoId).isFail()) {
      return false
    }

    return markTempFileDeleted(photoId)
  }

  private suspend fun markTempFileDeleted(id: Long): Boolean {
    val tempFileEntity = tempFilesDao.findById(id)
    if (tempFileEntity == null) {
      //has already been deleted
      return true
    }

    val time = timeUtils.getTimeFast()
    if (tempFilesDao.markDeletedById(id, time).isFail()) {
      return false
    }

    return true
  }
}