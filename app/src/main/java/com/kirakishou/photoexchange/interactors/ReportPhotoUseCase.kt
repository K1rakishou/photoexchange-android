package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.GalleryPhotoInfoEntity
import com.kirakishou.photoexchange.helper.database.repository.GalleryPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.helper.exception.EmptyUserIdException
import com.kirakishou.photoexchange.helper.util.TimeUtils
import kotlinx.coroutines.withContext
import java.lang.Exception

open class ReportPhotoUseCase(
  private val database: MyDatabase,
  private val apiClient: ApiClient,
  private val timeUtils: TimeUtils,
  private val settingsRepository: SettingsRepository,
  private val galleryPhotosRepository: GalleryPhotosRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "ReportPhotoUseCase"

  suspend fun reportPhoto(
    photoName: String
  ): Boolean {
    return withContext(coroutineContext) {
      val userId = settingsRepository.getUserId()
      if (userId.isEmpty()) {
        throw EmptyUserIdException()
      }

      return@withContext reportPhoto(userId, photoName)
    }
  }

  private suspend fun reportPhoto(userId: String, photoName: String): Boolean {
    val isReported = apiClient.reportPhoto(userId, photoName)

    try {
      reportInDatabase(photoName, isReported)
    } catch (error: Exception) {
      throw DatabaseException(error.message)
    }

    return isReported
  }

  private suspend fun reportInDatabase(photoName: String, isReported: Boolean) {
    database.transactional {
      val galleryPhotoEntity = galleryPhotosRepository.findPhotoByPhotoName(photoName)
      if (galleryPhotoEntity == null) {
        //TODO: should an exception be thrown here?
        return@transactional
      }

      var galleryPhotoInfoEntity = galleryPhotosRepository.findPhotoInfoByPhotoName(galleryPhotoEntity.photoName)
      if (galleryPhotoInfoEntity == null) {
        galleryPhotoInfoEntity = GalleryPhotoInfoEntity.create(
          galleryPhotoEntity.photoName,
          false,
          0,
          isReported,
          timeUtils.getTimeFast()
        )
      } else {
        galleryPhotoInfoEntity.isReported = isReported
      }

      if (!galleryPhotosRepository.save(galleryPhotoInfoEntity)) {
        throw DatabaseException("Could not update gallery photo info")
      }
    }
  }
}