package com.kirakishou.photoexchange.usecases

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.api.response.FavouritePhotoResponseData
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.GalleryPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.PhotoAdditionalInfoRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.helper.exception.EmptyUserUuidException
import com.kirakishou.photoexchange.helper.exception.NetworkAccessDisabledInSettings
import com.kirakishou.photoexchange.helper.util.NetUtils
import com.kirakishou.photoexchange.mvrx.model.FavouritePhotoActionResult
import com.kirakishou.photoexchange.mvrx.model.photo.PhotoAdditionalInfo
import kotlinx.coroutines.withContext
import timber.log.Timber

open class FavouritePhotoUseCase(
  private val apiClient: ApiClient,
  private val netUtils: NetUtils,
  private val settingsRepository: SettingsRepository,
  private val galleryPhotosRepository: GalleryPhotosRepository,
  private val photoAdditionalInfoRepository: PhotoAdditionalInfoRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "FavouritePhotoUseCase"

  suspend fun favouritePhoto(
    photoName: String
  ): FavouritePhotoActionResult {
    return withContext(coroutineContext) {
      val userUuid = settingsRepository.getUserUuid()
      if (userUuid.isEmpty()) {
        throw EmptyUserUuidException()
      }

      if (!netUtils.canAccessNetwork()) {
        throw NetworkAccessDisabledInSettings()
      }

      val favouritePhotoResult = apiClient.favouritePhoto(userUuid, photoName)

      try {
        favouriteInDatabase(photoName, favouritePhotoResult)
      } catch (error: Throwable) {
        Timber.tag(TAG).e(error, "Error while trying to favourite photo ($photoName) in database")
        throw DatabaseException(error.message)
      }

      return@withContext FavouritePhotoActionResult(
        photoName,
        favouritePhotoResult.isFavourited,
        favouritePhotoResult.favouritesCount
      )
    }
  }

  private suspend fun favouriteInDatabase(
    photoName: String,
    favouritePhotoResponseData: FavouritePhotoResponseData
  ) {
    val galleryPhotoEntity = galleryPhotosRepository.findPhotoByPhotoName(photoName)
    if (galleryPhotoEntity == null) {
      Timber.tag(TAG).d("Could not find photo with name ($photoName)")
      return
    }

    val photoAdditionalInfo = photoAdditionalInfoRepository.findByPhotoName(galleryPhotoEntity.photoName)
      //if additional photo info has been found - update it with new data
      ?.copy(
        isFavourited = favouritePhotoResponseData.isFavourited,
        favouritesCount = favouritePhotoResponseData.favouritesCount
      )
      //otherwise just create a new one for this photo
      ?: PhotoAdditionalInfo.create(
        galleryPhotoEntity.photoName,
        favouritePhotoResponseData.isFavourited,
        favouritePhotoResponseData.favouritesCount,
        false
      )

    if (!photoAdditionalInfoRepository.save(photoAdditionalInfo)) {
      throw DatabaseException("Could not update gallery photo info ")
    }
  }

}