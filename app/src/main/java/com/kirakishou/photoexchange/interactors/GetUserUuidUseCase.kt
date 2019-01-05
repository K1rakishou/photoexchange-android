package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.helper.exception.NetworkAccessDisabledInSettings
import com.kirakishou.photoexchange.helper.util.NetUtils
import kotlinx.coroutines.withContext

open class GetUserUuidUseCase(
  private val apiClient: ApiClient,
  private val netUtils: NetUtils,
  private val settingsRepository: SettingsRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "GetUserUuidUseCase"

  open suspend fun getUserId(): String {
    return withContext(coroutineContext) {
      val userId = settingsRepository.getUserUuid()
      if (userId.isNotEmpty()) {
        return@withContext userId
      }

      if (!netUtils.canAccessNetwork()) {
        throw NetworkAccessDisabledInSettings()
      }

      val newUserUuid = apiClient.getUserUuid()
      if (!settingsRepository.saveUserUuid(newUserUuid)) {
        throw DatabaseException("Could not update userId in the database")
      }

      return@withContext newUserUuid
    }
  }
}