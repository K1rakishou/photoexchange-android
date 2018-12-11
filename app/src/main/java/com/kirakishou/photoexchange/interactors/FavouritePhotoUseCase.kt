package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.api.response.FavouritePhotoResponseData
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.GalleryPhotoInfoEntity
import com.kirakishou.photoexchange.helper.database.repository.GalleryPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.helper.exception.EmptyUserIdException
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.FavouritePhotoActionResult
import kotlinx.coroutines.withContext

open class FavouritePhotoUseCase(
  private val database: MyDatabase,
  private val apiClient: ApiClient,
  private val timeUtils: TimeUtils,
  private val settingsRepository: SettingsRepository,
  private val galleryPhotosRepository: GalleryPhotosRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "FavouritePhotoUseCase"

  suspend fun favouritePhoto(
    photoName: String
  ): FavouritePhotoActionResult {
    return withContext(coroutineContext) {
      val userId = settingsRepository.getUserId()
      if (userId.isEmpty()) {
        throw EmptyUserIdException()
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
          favouritePhotoResponseData.isFavourited,
          favouritePhotoResponseData.favouritesCount,
          false,
          timeUtils.getTimeFast()
        )
      } else {
        galleryPhotoInfoEntity.isFavourited = favouritePhotoResponseData.isFavourited
      }

      if (!galleryPhotosRepository.save(galleryPhotoInfoEntity)) {
        throw DatabaseException("Could not update gallery photo info ")
      }
    }
  }

}