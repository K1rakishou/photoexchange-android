package com.kirakishou.photoexchange.usecases

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.source.remote.FirebaseRemoteSource
import com.kirakishou.photoexchange.helper.exception.ApiErrorException
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.helper.exception.FirebaseException
import com.kirakishou.photoexchange.helper.exception.NetworkAccessDisabledInSettings
import com.kirakishou.photoexchange.helper.util.NetUtils
import kotlinx.coroutines.withContext
import timber.log.Timber

open class UpdateFirebaseTokenUseCase(
  private val apiClient: ApiClient,
  private val netUtils: NetUtils,
  private val settingsRepository: SettingsRepository,
  private val firebaseRemoteSource: FirebaseRemoteSource,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "UpdateFirebaseTokenUseCase"

  open suspend fun updateFirebaseTokenIfNecessary() {
    return withContext(coroutineContext) {
      val newToken = settingsRepository.getNewFirebaseToken()
      val regularToken = settingsRepository.getFirebaseToken()

      //both tokens must be not empty, new token must be equal to regular token
      //otherwise we need to update token
      if (newToken.isNotEmpty() && regularToken.isNotEmpty() && newToken == regularToken) {
        return@withContext
      }

      if (!netUtils.canAccessNetwork()) {
        throw NetworkAccessDisabledInSettings()
      }

      return@withContext updateFirebaseTokenInternal()
    }
  }

  private suspend fun updateFirebaseTokenInternal() {
    //always retrieve fresh token from the firebase instead of reading it from the database
    val freshToken = try {
      firebaseRemoteSource.getTokenAsync()
    } catch (error: Throwable) {
      throw FirebaseException(error.message)
    }

    if (freshToken.isEmpty()) {
      throw FirebaseException("Token is empty!")
    }

    //update both the regular firebase token and the new one since we have just retrieved
    //the latest token directly from the firebase
    if (!settingsRepository.saveFirebaseToken(freshToken)) {
      throw DatabaseException("Could not update firebase firebase token")
    }

    if (!settingsRepository.saveNewFirebaseToken(freshToken)) {
      throw DatabaseException("Could not update new firebase firebase token")
    }

    val userUuid = settingsRepository.getUserUuid()
    if (userUuid.isEmpty()) {
      throw DatabaseException("Cannot update firebase because userUuid is empty!")
    }

    try {
      apiClient.updateFirebaseToken(userUuid, freshToken)
    } catch (error: ApiErrorException) {
      Timber.tag(TAG).e(error)

      //reset both tokens when unknown error occurred
      if (!settingsRepository.saveFirebaseToken(null)) {
        throw DatabaseException("Could not reset firebase token")
      }

      if (!settingsRepository.saveNewFirebaseToken(null)) {
        throw DatabaseException("Could not reset firebase token")
      }

      throw error
    }
  }
}