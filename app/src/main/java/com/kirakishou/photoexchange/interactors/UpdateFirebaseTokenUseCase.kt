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

  open suspend fun updateFirebaseToken(): Either<Exception, String> {
    return withContext(coroutineContext) {
      return@withContext myRunCatching {
        val newToken = try {
          firebaseRemoteSource.getTokenAsync().await()
        } catch (error: Throwable) {
          throw FirebaseException(error.message)
        }

        return@myRunCatching updateTokenInternal(newToken)
      }
    }
  }

  open suspend fun updateFirebaseToken(newToken: String?): Either<Exception, String> {
    return withContext(coroutineContext) {
      return@withContext myRunCatching {
        return@myRunCatching updateTokenInternal(newToken)
      }
    }
  }

  private suspend fun updateTokenInternal(newToken: String?): String {
    if (newToken.isNullOrEmpty()) {
      throw FirebaseException("Token is empty or null!")
    }

    if (!settingsRepository.saveFirebaseToken(newToken)) {
      throw DatabaseException("Could not store new firebase token")
    }

    val userId = settingsRepository.getUserId()
    if (userId.isEmpty()) {
      throw DatabaseException("Cannot update firebase because userId is empty!")
    }

    try {
      apiClient.updateFirebaseToken(userId, newToken)
    } catch (error: ApiErrorException) {
      Timber.tag(TAG).e(error)

      if (!settingsRepository.saveFirebaseToken(null)) {
        throw DatabaseException("Could not reset firebase token")
      }

      throw error
    }

    return newToken!!
  }
}