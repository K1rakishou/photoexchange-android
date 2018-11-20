package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.repository.GalleryPhotoRepository
import com.kirakishou.photoexchange.helper.myRunCatching
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

open class FavouritePhotoUseCase(
  private val apiClient: ApiClient,
  private val galleryPhotoRepository: GalleryPhotoRepository
) {
  private val TAG = "FavouritePhotoUseCase"

  suspend fun favouritePhoto(userId: String, photoName: String): Either<Exception, FavouritePhotoResult> {
    return withContext(coroutineContext) {
      return@withContext myRunCatching {
        val favouritePhotoResult = apiClient.favouritePhoto(userId, photoName)

        galleryPhotoRepository.favouritePhoto(
          photoName,
          favouritePhotoResult.isFavourited,
          favouritePhotoResult.favouritesCount
        )

        return@myRunCatching favouritePhotoResult
      }
    }
  }

  data class FavouritePhotoResult(val isFavourited: Boolean,
                                  val favouritesCount: Long)
}