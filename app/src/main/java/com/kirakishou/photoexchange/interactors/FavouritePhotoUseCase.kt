package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.FavouritePhotoRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.exception.EmptyUserIdException
import com.kirakishou.photoexchange.helper.myRunCatching
import com.kirakishou.photoexchange.mvp.model.FavouritePhotoActionResult
import kotlinx.coroutines.withContext

open class FavouritePhotoUseCase(
  private val settingsRepository: SettingsRepository,
  private val favouritePhotoRepository: FavouritePhotoRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "FavouritePhotoUseCase"

  suspend fun favouritePhoto(
    photoName: String
  ): Either<Exception, FavouritePhotoActionResult> {
    return withContext(coroutineContext) {
      return@withContext myRunCatching {
        val userId = settingsRepository.getUserId()
        if (userId.isEmpty()) {
          throw EmptyUserIdException()
        }

        return@myRunCatching favouritePhotoRepository.favouritePhoto(userId, photoName)
      }
    }
  }

}