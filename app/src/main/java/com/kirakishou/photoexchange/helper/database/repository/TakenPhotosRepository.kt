package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.TakenPhotoEntity
import com.kirakishou.photoexchange.helper.database.entity.TempFileEntity
import com.kirakishou.photoexchange.helper.database.isFail
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.database.mapper.TakenPhotosMapper
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Created by kirakishou on 3/3/2018.
 */
open class TakenPhotosRepository(
  private val timeUtils: TimeUtils,
  private val database: MyDatabase,
  private val tempFileRepository: TempFileRepository,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {
  private val TAG = "TakenPhotosRepository"
  private val takenPhotoDao = database.takenPhotoDao()

  init {
    runBlocking(coroutineContext) {
      tempFileRepository.init()
    }
  }

  suspend fun saveTakenPhoto(tempFile: TempFileEntity): TakenPhoto {
    return withContext(coroutineContext) {
      var photo = TakenPhoto.empty()

      val transactionResult = database.transactional {
        val myPhotoEntity = TakenPhotoEntity.create(tempFile.id!!, false, timeUtils.getTimeFast())
        val insertedPhotoId = takenPhotoDao.insert(myPhotoEntity)

        if (insertedPhotoId.isFail()) {
          return@transactional false
        }

        myPhotoEntity.id = insertedPhotoId
        photo = TakenPhotosMapper.toMyPhoto(myPhotoEntity, tempFile)

        return@transactional tempFileRepository.updateTakenPhotoId(tempFile, insertedPhotoId).isSuccess()
      }

      if (!transactionResult) {
        tempFileRepository.markDeletedById(tempFile)
        return@withContext TakenPhoto.empty()
      }

      return@withContext photo
    }
  }

  suspend fun updateMakePhotoPublic(takenPhotoId: Long): Boolean {
    return withContext(coroutineContext) {
      return@withContext takenPhotoDao.updateSetPhotoPublic(takenPhotoId) == 1
    }
  }

  suspend fun updateAllPhotosLocation(location: LonLat) {
    return withContext(coroutineContext) {
      if (location.isEmpty()) {
        return@withContext
      }

      val allPhotosWithEmptyLocation = takenPhotoDao.findAllWithEmptyLocation()
      if (allPhotosWithEmptyLocation.isEmpty()) {
        return@withContext
      }

      database.transactional {
        for (photo in allPhotosWithEmptyLocation) {
          if (takenPhotoDao.updatePhotoLocation(photo.id!!, location.lon, location.lat) != 1) {
            return@transactional false
          }
        }

        return@transactional true
      }
    }
  }

  suspend fun hasPhotosWithEmptyLocation(): Boolean {
    return withContext(coroutineContext) {
      return@withContext takenPhotoDao.findAllWithEmptyLocation().isNotEmpty()
    }
  }

  suspend fun findById(id: Long): TakenPhoto {
    return withContext(coroutineContext) {
      val myPhotoEntity = takenPhotoDao.findById(id) ?: TakenPhotoEntity.empty()
      val tempFileEntity = findTempFileById(id)

      return@withContext TakenPhotosMapper.toMyPhoto(myPhotoEntity, tempFileEntity)
    }
  }

  suspend fun findAll(): List<TakenPhoto> {
    return withContext(coroutineContext) {
      val allMyPhotos = arrayListOf<TakenPhoto>()
      val allMyPhotoEntities = takenPhotoDao.findAll()

      for (myPhotoEntity in allMyPhotoEntities) {
        myPhotoEntity.id?.let { myPhotoId ->
          val tempFile = findTempFileById(myPhotoId)

          TakenPhotosMapper.toMyPhoto(myPhotoEntity, tempFile).let { myPhoto ->
            allMyPhotos += myPhoto
          }
        }
      }

      return@withContext allMyPhotos
    }
  }

  open suspend fun countAllByState(state: PhotoState): Int {
    return withContext(coroutineContext) {
      return@withContext takenPhotoDao.countAllByState(state).toInt()
    }
  }

  open suspend fun updateStates(oldState: PhotoState, newState: PhotoState) {
    withContext(coroutineContext) {
      takenPhotoDao.updateStates(oldState, newState)
    }
  }

  open suspend fun findAllByState(state: PhotoState): List<TakenPhoto> {
    return withContext(coroutineContext) {
      val resultList = mutableListOf<TakenPhoto>()

      database.transactional {
        val allPhotoReadyToUploading = takenPhotoDao.findAllWithState(state)

        for (photo in allPhotoReadyToUploading) {
          val tempFileEntity = findTempFileById(photo.id!!)
          resultList += TakenPhotosMapper.toMyPhoto(photo, tempFileEntity)
        }

        return@transactional true
      }

      return@withContext resultList
    }
  }

  suspend fun deleteMyPhoto(takenPhoto: TakenPhoto?): Boolean {
    return withContext(coroutineContext) {
      if (takenPhoto == null) {
        return@withContext true
      }

      if (takenPhoto.isEmpty()) {
        return@withContext true
      }

      return@withContext deletePhotoById(takenPhoto.id)
    }
  }

  suspend fun deletePhotoByName(photoName: String): Boolean {
    return withContext(coroutineContext) {
      val photoId = takenPhotoDao.findPhotoIdByName(photoName)
      if (photoId == null) {
        //already deleted
        return@withContext true
      }

      return@withContext deletePhotoById(photoId)
    }
  }

  private suspend fun findTempFileById(id: Long): TempFileEntity {
    return withContext(coroutineContext) {
      return@withContext tempFileRepository.findById(id)
    }
  }

  suspend fun findTempFile(id: Long): TempFileEntity {
    return withContext(coroutineContext) {
      return@withContext tempFileRepository.findById(id)
    }
  }

  suspend fun tryToFixStalledPhotos() {
    return withContext(coroutineContext) {
      val stillUploadingPhotos = findAllByState(PhotoState.PHOTO_UPLOADING)

      for (photo in stillUploadingPhotos) {
        if (!photo.fileExists()) {
          deletePhotoById(photo.id)
          continue
        }

        updatePhotoState(photo.id, PhotoState.FAILED_TO_UPLOAD)
      }
    }
  }

  //TODO: tests
  suspend fun cleanup() {
    withContext(coroutineContext) {
      database.transactional {
        //we need to delete all photos with state PHOTO_TAKEN because at this step they are being considered corrupted
        if (!deleteAllWithState(PhotoState.PHOTO_TAKEN)) {
          return@transactional false
        }

        //delete photo files that were marked as deleted earlier than (CURRENT_TIME - OLD_PHOTO_TIME_THRESHOLD)
        tempFileRepository.deleteOld()

        //delete photo files that have no takenPhotoId
        tempFileRepository.deleteEmptyTempFiles()

        //in case the user takes photos way too often and they weight a lot (like 3-4 mb per photo)
        //we need to consider this as well so we delete them when total files size exceeds MAX_CACHE_SIZE
        tempFileRepository.deleteOldIfCacheSizeIsTooBig()

        return@transactional true
      }
    }
  }

  private suspend fun deleteAllWithState(photoState: PhotoState): Boolean {
    return withContext(coroutineContext) {
      return@withContext database.transactional {
        val myPhotosList = takenPhotoDao.findAllWithState(photoState)

        for (myPhoto in myPhotosList) {
          if (!deletePhotoById(myPhoto.id!!)) {
            return@transactional false
          }
        }

        return@transactional true
      }
    }
  }
}