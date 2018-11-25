package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.myRunCatching
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import kotlinx.coroutines.withContext

open class GetUserIdUseCase(
  private val settingsRepository: SettingsRepository,
  private val apiClient: ApiClient,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "GetUserIdUseCase"

  open suspend fun getUserId(): Either<Exception, String> {
    return withContext(coroutineContext) {
      return@withContext myRunCatching {
        val userId = settingsRepository.getUserId()
        if (userId.isNotEmpty()) {
          return@myRunCatching userId
        }

        val newUserId = apiClient.getUserId()
        if (!settingsRepository.saveUserId(newUserId)) {
          throw DatabaseException("Could not update userId in the database")
        }

        return@myRunCatching newUserId
      }
    }
  }
}