package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.helper.exception.NetworkAccessDisabledInSettings
import com.kirakishou.photoexchange.helper.util.NetUtils
import kotlinx.coroutines.withContext

open class GetUserIdUseCase(
  private val apiClient: ApiClient,
  private val netUtils: NetUtils,
  private val settingsRepository: SettingsRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "GetUserIdUseCase"

  open suspend fun getUserId(): String {
    return withContext(coroutineContext) {
      val userId = settingsRepository.getUserId()
      if (userId.isNotEmpty()) {
        return@withContext userId
      }

      if (!netUtils.canAccessNetwork()) {
        throw NetworkAccessDisabledInSettings()
      }

      val newUserId = apiClient.getUserId()
      if (!settingsRepository.saveUserId(newUserId)) {
        throw DatabaseException("Could not update userId in the database")
      }

      return@withContext newUserId
    }
  }
}