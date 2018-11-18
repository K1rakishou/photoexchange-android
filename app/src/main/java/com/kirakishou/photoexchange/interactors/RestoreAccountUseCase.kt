package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.myRunCatching
import com.kirakishou.photoexchange.mvp.model.exception.DatabaseException
import kotlinx.coroutines.withContext

open class RestoreAccountUseCase(
  private val apiClient: ApiClient,
  private val database: MyDatabase,
  private val settingsRepository: SettingsRepository,
  private val uploadedPhotosRepository: UploadedPhotosRepository,
  private val receivedPhotosRepository: ReceivedPhotosRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {

  suspend fun restoreAccount(oldUserId: String): Either<Exception, Boolean> {
    return withContext(coroutineContext) {
      return@withContext myRunCatching {
        val accountExists = apiClient.checkAccountExists(oldUserId)
        if (!accountExists) {
          return@myRunCatching false
        }

        val transactionResult = cleanDatabase(oldUserId)
        if (!transactionResult) {
          throw DatabaseException("Could not clean database")
        }

        return@myRunCatching true
      }
    }
  }

  private suspend fun cleanDatabase(oldUserId: String): Boolean {
    return database.transactional {
      if (!settingsRepository.saveUserId(oldUserId)) {
        return@transactional false
      }

      uploadedPhotosRepository.deleteAll()
      receivedPhotosRepository.deleteAll()

      return@transactional true
    }
  }
}