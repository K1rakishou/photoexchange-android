package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.ReportPhotoRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.exception.EmptyUserIdException
import com.kirakishou.photoexchange.helper.myRunCatching
import kotlinx.coroutines.withContext

open class ReportPhotoUseCase(
  private val settingsRepository: SettingsRepository,
  private val reportPhotoRepository: ReportPhotoRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "ReportPhotoUseCase"

  suspend fun reportPhoto(
    photoName: String
  ): Either<Exception, Boolean> {
    return withContext(coroutineContext) {
      return@withContext myRunCatching {
        val userId = settingsRepository.getUserId()
        if (userId.isEmpty()) {
          throw EmptyUserIdException()
        }

        return@myRunCatching reportPhotoRepository.reportPhoto(userId, photoName)
      }
    }
  }
}