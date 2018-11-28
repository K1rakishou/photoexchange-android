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
import core.ErrorCode
import kotlinx.coroutines.withContext
import java.lang.Exception

open class GetFirebaseTokenUseCase(
  private val settingsRepository: SettingsRepository,
  private val firebaseRemoteSource: FirebaseRemoteSource,
  private val apiClient: ApiClient,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {

  open suspend fun getFirebaseToken(): Either<Exception, String> {
    return withContext(coroutineContext) {
      return@withContext myRunCatching {
        val token = settingsRepository.getFirebaseToken()
        if (token.isNotEmpty()) {
          return@myRunCatching token
        }

        val newToken = try {
          firebaseRemoteSource.getTokenAsync().await()
        } catch (error: Throwable) {
          throw FirebaseException(error.message)
        }

        if (newToken.isNullOrEmpty()) {
          throw FirebaseException("Firebase returned empty token!")
        }

        if (!settingsRepository.saveFirebaseToken(newToken)) {
          throw DatabaseException("Could not store new firebase token")
        }

        val userId = settingsRepository.getUserId()
        if (userId.isEmpty()) {
          throw DatabaseException("Cannot update firebase because userId is empty!")
        }

        //TODO: change result type
        if (!apiClient.updateFirebaseToken(userId, newToken)) {
          settingsRepository.saveFirebaseToken(null)
          //TODO: add errorCode
          throw ApiErrorException(ErrorCode.UnknownError)
        }

        return@myRunCatching newToken!!
      }
    }
  }
}