package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.TakenPhotoEntity
import com.kirakishou.photoexchange.helper.database.entity.TempFileEntity
import com.kirakishou.photoexchange.helper.database.isFail
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.database.mapper.TakenPhotosMapper
import com.kirakishou.photoexchange.helper.database.source.local.TakenPhotosLocalSource
import com.kirakishou.photoexchange.helper.database.source.local.TempFileLocalSource
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.photo.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.helper.exception.DatabaseException

/**
 * Created by kirakishou on 3/3/2018.
 */
open class TakenPhotosRepository(
  private val timeUtils: TimeUtils,
  private val database: MyDatabase,
  private val takenPhotosLocalSource: TakenPhotosLocalSource,
  private val tempFileLocalSource: TempFileLocalSource
) : BaseRepository() {
  private val TAG = "TakenPhotosRepository"

  init {
    tempFileLocalSource.init()
  }

  suspend fun createTempFile(): TempFileEntity {
    return tempFileLocalSource.create()
  }

  suspend fun markDeletedById(tempFile: TempFileEntity) {
    tempFileLocalSource.markDeletedById(tempFile)
  }

  suspend fun saveTakenPhoto(tempFile: TempFileEntity): TakenPhoto? {
    var photo: TakenPhoto? = null

    val transactionResult = database.transactional {
      val myPhotoEntity = TakenPhotoEntity.create(tempFile.id!!, false, timeUtils.getTimeFast())
      val insertedPhotoId = takenPhotosLocalSource.save(myPhotoEntity)

      if (insertedPhotoId.isFail()) {
        return@transactional false
      }

      myPhotoEntity.id = insertedPhotoId
      photo = TakenPhotosMapper.toTakenPhoto(myPhotoEntity, tempFile)

      return@transactional tempFileLocalSource.updateTakenPhotoId(tempFile, insertedPhotoId).isSuccess()
    }

    if (!transactionResult) {
      tempFileLocalSource.markDeletedById(tempFile)
      return null
    }

    return photo
  }

  suspend fun updatePhotoState(photoId: Long, newPhotoState: PhotoState): Boolean {
    return takenPhotosLocalSource.updatePhotoState(photoId, newPhotoState)
  }

  suspend fun updateMakePhotoPublic(takenPhotoId: Long, makePublic: Boolean): Boolean {
    return if (makePublic) {
      takenPhotosLocalSource.updateSetPhotoPublic(takenPhotoId)
    } else {
      takenPhotosLocalSource.updateSetPhotoPrivate(takenPhotoId)
    }
  }

  suspend fun hasPhotosWithEmptyLocation(): Boolean {
    return takenPhotosLocalSource.findAllWithEmptyLocation().isNotEmpty()
  }

  suspend fun findById(id: Long): TakenPhoto? {
    val myPhotoEntity = takenPhotosLocalSource.findById(id)
    val tempFileEntity = tempFileLocalSource.findById(id)

    return TakenPhotosMapper.toTakenPhoto(myPhotoEntity, tempFileEntity)
  }

  suspend fun findAll(): List<TakenPhoto> {
    val allMyPhotos = arrayListOf<TakenPhoto>()
    val allMyPhotoEntities = takenPhotosLocalSource.findAll()

    for (myPhotoEntity in allMyPhotoEntities) {
      myPhotoEntity.id?.let { myPhotoId ->
        val tempFile = tempFileLocalSource.findById(myPhotoId)

        TakenPhotosMapper.toTakenPhoto(myPhotoEntity, tempFile)?.let { myPhoto ->
          allMyPhotos += myPhoto
        }
      }
    }

    return allMyPhotos
  }

  suspend fun findAllWithEmptyLocation(): List<TakenPhotoEntity> {
    return takenPhotosLocalSource.findAllWithEmptyLocation()
  }

  open suspend fun countAllByState(state: PhotoState): Int {
    return takenPhotosLocalSource.countAllByState(state)
  }

  open suspend fun updateStates(oldState: PhotoState, newState: PhotoState) {
    takenPhotosLocalSource.updateStates(oldState, newState)
  }

  suspend fun updatePhotoLocation(photoId: Long, lon: Double, lat: Double): Boolean {
    return takenPhotosLocalSource.updatePhotoLocation(photoId, lon, lat)
  }

  open suspend fun findAllByState(state: PhotoState): List<TakenPhoto> {
    val resultList = mutableListOf<TakenPhoto>()
    val allPhotoReadyToUploading = takenPhotosLocalSource.findAllWithState(state)

    for (photo in allPhotoReadyToUploading) {
      val tempFileEntity = tempFileLocalSource.findById(photo.id!!)
      val myPhoto = TakenPhotosMapper.toTakenPhoto(photo, tempFileEntity)

      if (myPhoto != null) {
        resultList += myPhoto
      }
    }

    return resultList
      .sortedByDescending { it.id }
  }

  suspend fun deletePhotoByName(photoName: String): Boolean {
    val takenPhoto = takenPhotosLocalSource.findPhotoByName(photoName)
    if (takenPhoto.isEmpty()) {
      //already deleted
      return true
    }

    return deletePhotoById(takenPhoto.id!!)
  }

  suspend fun deleteMyPhoto(takenPhoto: TakenPhoto?): Boolean {
    if (takenPhoto == null) {
      return true
    }

    return deletePhotoById(takenPhoto.id)
  }

  suspend fun deletePhotoById(photoId: Long): Boolean {
    return takenPhotosLocalSource.deletePhotoById(photoId)
  }

  suspend fun findTempFile(id: Long): TempFileEntity {
    return tempFileLocalSource.findById(id)
  }

  open suspend fun loadNotUploadedPhotos(): List<TakenPhoto> {
    val stillUploadingPhotos = findAllByState(PhotoState.PHOTO_UPLOADING)

    database.transactional {
      for (photo in stillUploadingPhotos) {
        if (!photo.fileExists()) {
          takenPhotosLocalSource.deletePhotoById(photo.id)
          continue
        }

        takenPhotosLocalSource.updatePhotoState(photo.id, PhotoState.PHOTO_QUEUED_UP)
      }
    }

    return findAllByState(PhotoState.PHOTO_QUEUED_UP)
  }

  //TODO: tests
  suspend fun cleanup() {
    database.transactional {
      //we need to delete all photos with state PHOTO_TAKEN because at this step they are being considered corrupted
      val myPhotosList = takenPhotosLocalSource.findAllWithState(PhotoState.PHOTO_TAKEN)
      for (myPhoto in myPhotosList) {
        if (!takenPhotosLocalSource.deletePhotoById(myPhoto.id!!)) {
          throw DatabaseException("Could not delete photo with id ${myPhoto.id!!}")
        }
      }

      //delete photo files that were marked as deleted earlier than (CURRENT_TIME - OLD_PHOTO_TIME_THRESHOLD)
      tempFileLocalSource.deleteOld()

      //delete photo files that have no takenPhotoId
      tempFileLocalSource.deleteEmptyTempFiles()

      //in case the user takes photos way too often and they weight a lot (like 3-4 mb per photo)
      //we need to consider this as well so we delete them when total files size exceeds MAX_CACHE_SIZE
      tempFileLocalSource.deleteOldIfCacheSizeIsTooBig()
    }
  }
}