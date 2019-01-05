package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.GalleryPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.PhotoAdditionalInfoRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.helper.exception.EmptyUserIdException
import com.kirakishou.photoexchange.helper.exception.NetworkAccessDisabledInSettings
import com.kirakishou.photoexchange.helper.util.NetUtils
import com.kirakishou.photoexchange.mvp.model.photo.PhotoAdditionalInfo
import kotlinx.coroutines.withContext
import java.lang.Exception

open class ReportPhotoUseCase(
  private val apiClient: ApiClient,
  private val netUtils: NetUtils,
  private val settingsRepository: SettingsRepository,
  private val galleryPhotosRepository: GalleryPhotosRepository,
  private val photoAdditionalInfoRepository: PhotoAdditionalInfoRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "ReportPhotoUseCase"

  suspend fun reportPhoto(
    photoName: String
  ): Boolean {
    return withContext(coroutineContext) {
      val userId = settingsRepository.getUserUuid()
      if (userId.isEmpty()) {
        throw EmptyUserIdException()
      }

      if (!netUtils.canAccessNetwork()) {
        throw NetworkAccessDisabledInSettings()
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
    val galleryPhotoEntity = galleryPhotosRepository.findPhotoByPhotoName(photoName)
    if (galleryPhotoEntity == null) {
      return
    }

    val photoAdditionalInfo = photoAdditionalInfoRepository.findByPhotoName(galleryPhotoEntity.photoName)
      ?.copy(
        isReported = isReported
      )
      ?: PhotoAdditionalInfo.create(
        galleryPhotoEntity.photoName,
        false,
        0,
        isReported
      )

    if (!photoAdditionalInfoRepository.save(photoAdditionalInfo)) {
      throw DatabaseException("Could not update photo additional info")
    }
  }
}