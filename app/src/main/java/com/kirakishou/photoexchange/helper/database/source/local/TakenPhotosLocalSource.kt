package com.kirakishou.photoexchange.helper.database.source.local

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.TakenPhotoEntity
import com.kirakishou.photoexchange.helper.database.isFail
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvrx.model.PhotoState

open class TakenPhotosLocalSource(
  private val database: MyDatabase,
  private val timeUtils: TimeUtils
) {
  private val takenPhotoDao = database.takenPhotoDao()
  private val tempFilesDao = database.tempFileDao()

  fun save(myPhotoEntity: TakenPhotoEntity): Long {
    return takenPhotoDao.insert(myPhotoEntity)
  }

  fun findById(photoId: Long): TakenPhotoEntity {
    return takenPhotoDao.findById(photoId) ?: TakenPhotoEntity.empty()
  }

  fun findPhotoByName(photoName: String): TakenPhotoEntity {
    return takenPhotoDao.findByName(photoName) ?: TakenPhotoEntity.empty()
  }

  fun findAll(): List<TakenPhotoEntity> {
    return takenPhotoDao.findAll()
  }

  fun findAllWithEmptyLocation(): List<TakenPhotoEntity> {
    return takenPhotoDao.findAllWithEmptyLocation()
  }

  fun findAllWithState(state: PhotoState): List<TakenPhotoEntity> {
    return takenPhotoDao.findAllWithState(state)
  }

  fun countAllByState(state: PhotoState): Int {
    return takenPhotoDao.countAllByState(state).toInt()
  }

  open fun updatePhotoState(photoId: Long, newPhotoState: PhotoState): Boolean {
    return takenPhotoDao.updateSetNewPhotoState(photoId, newPhotoState) == 1
  }

  fun updateStates(oldState: PhotoState, newState: PhotoState) {
    takenPhotoDao.updateStates(oldState, newState)
  }

  fun updateSetPhotoPublic(takenPhotoId: Long): Boolean {
    return takenPhotoDao.updateSetPhotoPublic(takenPhotoId) == 1
  }

  fun updateSetPhotoPrivate(takenPhotoId: Long): Boolean {
    return takenPhotoDao.updateSetPhotoPrivate(takenPhotoId) == 1
  }

  fun updatePhotoLocation(photoId: Long, lon: Double, lat: Double): Boolean {
    return takenPhotoDao.updatePhotoLocation(photoId, lon, lat) == 1
  }

  open fun deletePhotoById(photoId: Long): Boolean {
    if (takenPhotoDao.findById(photoId) == null) {
      //already deleted
      return true
    }

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