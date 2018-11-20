package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.response.FavouritePhotoResponseData
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.FavouritePhotoRepository
import com.kirakishou.photoexchange.helper.myRunCatching
import kotlinx.coroutines.withContext

open class FavouritePhotoUseCase(
  private val favouritePhotoRepository: FavouritePhotoRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "FavouritePhotoUseCase"

  suspend fun favouritePhoto(
    userId: String,
    photoName: String
  ): Either<Exception, FavouritePhotoResponseData> {
    return withContext(coroutineContext) {
      return@withContext myRunCatching {
        return@myRunCatching favouritePhotoRepository.favouritePhoto(userId, photoName)
      }
    }
  }

}