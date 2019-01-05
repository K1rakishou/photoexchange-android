package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.api.response.FavouritePhotoResponseData
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.GalleryPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.PhotoAdditionalInfoRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.helper.exception.EmptyUserIdException
import com.kirakishou.photoexchange.helper.exception.NetworkAccessDisabledInSettings
import com.kirakishou.photoexchange.helper.util.NetUtils
import com.kirakishou.photoexchange.mvp.model.FavouritePhotoActionResult
import com.kirakishou.photoexchange.mvp.model.photo.PhotoAdditionalInfo
import kotlinx.coroutines.withContext

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
      val userId = settingsRepository.getUserUuid()
      if (userId.isEmpty()) {
        throw EmptyUserIdException()
      }

      if (!netUtils.canAccessNetwork()) {
        throw NetworkAccessDisabledInSettings()
      }

      val favouritePhotoResult = apiClient.favouritePhoto(userId, photoName)

      try {
        favouriteInDatabase(photoName, favouritePhotoResult)
      } catch (error: Throwable) {
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
      return
    }

    val photoAdditionalInfo = photoAdditionalInfoRepository.findByPhotoName(galleryPhotoEntity.photoName)
      ?.copy(
        isFavourited = favouritePhotoResponseData.isFavourited,
        favouritesCount = favouritePhotoResponseData.favouritesCount
      )
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