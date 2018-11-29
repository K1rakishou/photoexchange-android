package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.source.remote.FirebaseRemoteSource
import com.kirakishou.photoexchange.helper.exception.ApiErrorException
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.helper.exception.FirebaseException
import com.kirakishou.photoexchange.helper.myRunCatching
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.Exception

open class UpdateFirebaseTokenUseCase(
  private val settingsRepository: SettingsRepository,
  private val firebaseRemoteSource: FirebaseRemoteSource,
  private val apiClient: ApiClient,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "UpdateFirebaseTokenUseCase"

  open suspend fun updateFirebaseTokenIfNecessary(): Either<Exception, String> {
    return withContext(coroutineContext) {
      return@withContext myRunCatching {
        val newToken = settingsRepository.getNewFirebaseToken()
        val regularToken = settingsRepository.getFirebaseToken()

        //both tokens must be not empty, new token must be equal to regular token
        //otherwise we need to update token
        if (newToken.isNotEmpty() && regularToken.isNotEmpty() && newToken == regularToken) {
          return@myRunCatching regularToken
        }

        return@myRunCatching updateFirebaseTokenInternal(newToken)
      }
    }
  }

  private suspend fun updateFirebaseTokenInternal(newToken: String): String {
    //always retrieve fresh token from the firebase instead of reading it from the database
    val freshToken = try {
      firebaseRemoteSource.getTokenAsync().await()
    } catch (error: Throwable) {
      throw FirebaseException(error.message)
    }

    if (freshToken.isNullOrEmpty()) {
      throw FirebaseException("Token is empty or null!")
    }

    //update both the regular firebase token and the new one since we have just retrieved
    //the latest token directly from the firebase
    if (!settingsRepository.saveFirebaseToken(freshToken)) {
      throw DatabaseException("Could not update firebase firebase token")
    }

    if (!settingsRepository.saveNewFirebaseToken(freshToken)) {
      throw DatabaseException("Could not update new firebase firebase token")
    }

    val userId = settingsRepository.getUserId()
    if (userId.isEmpty()) {
      throw DatabaseException("Cannot update firebase because userId is empty!")
    }

    try {
      apiClient.updateFirebaseToken(userId, newToken)
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

    return newToken
  }
}