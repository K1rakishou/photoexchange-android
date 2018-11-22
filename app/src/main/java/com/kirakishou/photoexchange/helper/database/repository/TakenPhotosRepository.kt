package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.TakenPhotoEntity
import com.kirakishou.photoexchange.helper.database.entity.TempFileEntity
import com.kirakishou.photoexchange.helper.database.isFail
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.database.mapper.TakenPhotosMapper
import com.kirakishou.photoexchange.helper.database.source.local.TakenPhotosLocalSource
import com.kirakishou.photoexchange.helper.database.source.local.TempFileLocalSource
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.exception.DatabaseException
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import com.kirakishou.photoexchange.mvp.viewmodel.UploadedPhotosFragmentViewModel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Created by kirakishou on 3/3/2018.
 */
open class TakenPhotosRepository(
  private val timeUtils: TimeUtils,
  private val database: MyDatabase,
  private val takenPhotosLocalSource: TakenPhotosLocalSource,
  private val tempFileLocalSource: TempFileLocalSource,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {
  private val TAG = "TakenPhotosRepository"
  private val takenPhotoDao = database.takenPhotoDao()

  init {
    runBlocking(coroutineContext) {
      tempFileLocalSource.init()
    }
  }

  suspend fun createTempFile(): TempFileEntity {
    return withContext(coroutineContext) {
      return@withContext tempFileLocalSource.create()
    }
  }

  suspend fun markDeletedById(tempFile: TempFileEntity) {
    withContext(coroutineContext) {
      tempFileLocalSource.markDeletedById(tempFile)
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

        return@transactional tempFileLocalSource.updateTakenPhotoId(tempFile, insertedPhotoId).isSuccess()
      }

      if (!transactionResult) {
        tempFileLocalSource.markDeletedById(tempFile)
        return@withContext TakenPhoto.empty()
      }

      return@withContext photo
    }
  }

  suspend fun updatePhotoState(photoId: Long, newPhotoState: PhotoState): Boolean {
    return withContext(coroutineContext) {
      return@withContext takenPhotosLocalSource.updatePhotoState(photoId, newPhotoState)
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
      val tempFileEntity = tempFileLocalSource.findById(id)

      return@withContext TakenPhotosMapper.toMyPhoto(myPhotoEntity, tempFileEntity)
    }
  }

  suspend fun findAll(): List<TakenPhoto> {
    return withContext(coroutineContext) {
      val allMyPhotos = arrayListOf<TakenPhoto>()
      val allMyPhotoEntities = takenPhotoDao.findAll()

      for (myPhotoEntity in allMyPhotoEntities) {
        myPhotoEntity.id?.let { myPhotoId ->
          val tempFile = tempFileLocalSource.findById(myPhotoId)

          TakenPhotosMapper.toMyPhoto(myPhotoEntity, tempFile).let { myPhoto ->
            allMyPhotos += myPhoto
          }
        }
      }

      return@withContext allMyPhotos
    }
  }

  open suspend fun figureOutWhatPhotosToLoad(): PhotosToLoad {
    return withContext(coroutineContext) {
      val hasFailedToUploadPhotos = countAllByState(PhotoState.FAILED_TO_UPLOAD) > 0
      val hasQueuedUpPhotos = countAllByState(PhotoState.PHOTO_QUEUED_UP) > 0

      if (hasFailedToUploadPhotos || hasQueuedUpPhotos) {
        return@withContext PhotosToLoad.QueuedUpAndFailed
      }

      return@withContext PhotosToLoad.Uploaded
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
      val allPhotoReadyToUploading = takenPhotoDao.findAllWithState(state)

      for (photo in allPhotoReadyToUploading) {
        val tempFileEntity = tempFileLocalSource.findById(photo.id!!)
        resultList += TakenPhotosMapper.toMyPhoto(photo, tempFileEntity)
      }

      return@withContext resultList
    }.sortedByDescending { it.id }
  }

  suspend fun deleteMyPhoto(takenPhoto: TakenPhoto?): Boolean {
    return withContext(coroutineContext) {
      if (takenPhoto == null) {
        return@withContext true
      }

      if (takenPhoto.isEmpty()) {
        return@withContext true
      }

      return@withContext takenPhotosLocalSource.deletePhotoById(takenPhoto.id)
    }
  }

  suspend fun deletePhotoById(photoId: Long): Boolean {
    return withContext(coroutineContext) {
      takenPhotosLocalSource.deletePhotoById(photoId)
    }
  }

  suspend fun findTempFile(id: Long): TempFileEntity {
    return withContext(coroutineContext) {
      return@withContext tempFileLocalSource.findById(id)
    }
  }

  suspend fun resetStalledPhotosState() {
    return withContext(coroutineContext) {
      val stillUploadingPhotos = findAllByState(PhotoState.PHOTO_UPLOADING)

      for (photo in stillUploadingPhotos) {
        if (!photo.fileExists()) {
          takenPhotosLocalSource.deletePhotoById(photo.id)
          continue
        }

        takenPhotosLocalSource.updatePhotoState(photo.id, PhotoState.FAILED_TO_UPLOAD)
      }
    }
  }

  //TODO: tests
  suspend fun cleanup() {
    withContext(coroutineContext) {
      database.transactional {
        //we need to delete all photos with state PHOTO_TAKEN because at this step they are being considered corrupted
        val myPhotosList = takenPhotoDao.findAllWithState(PhotoState.PHOTO_TAKEN)
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

        return@transactional true
      }
    }
  }

  enum class PhotosToLoad {
    QueuedUpAndFailed,
    Uploaded
  }
}