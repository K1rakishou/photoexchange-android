package com.kirakishou.photoexchange.usecases

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.GalleryPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.PhotoAdditionalInfoRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.helper.exception.EmptyUserUuidException
import com.kirakishou.photoexchange.helper.exception.NetworkAccessDisabledInSettings
import com.kirakishou.photoexchange.helper.util.NetUtils
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvrx.model.photo.PhotoAdditionalInfo
import kotlinx.coroutines.withContext
import timber.log.Timber
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
      val userUuid = settingsRepository.getUserUuid()
      if (userUuid.isEmpty()) {
        throw EmptyUserUuidException()
      }

      if (!netUtils.canAccessNetwork()) {
        throw NetworkAccessDisabledInSettings()
      }

      return@withContext reportPhoto(userUuid, photoName)
    }
  }

  private suspend fun reportPhoto(userUuid: String, photoName: String): Boolean {
    val isReported = apiClient.reportPhoto(userUuid, photoName)

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
      Timber.tag(TAG).d("Could not find photo with name ($photoName)")
      return
    }

    val photoAdditionalInfo = photoAdditionalInfoRepository.findByPhotoName(galleryPhotoEntity.photoName)
      //if additional photo info has been found - update it with new data
      ?.copy(
        isReported = isReported
      )
      //otherwise just create a new one for this photo
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