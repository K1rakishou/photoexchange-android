package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.RestoreAccountRepository
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import kotlinx.coroutines.withContext

open class RestoreAccountUseCase(
  private val apiClient: ApiClient,
  private val restoreAccountRepository: RestoreAccountRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {

  suspend fun restoreAccount(oldUserId: String): Boolean {
    return withContext(coroutineContext) {
      val accountExists = apiClient.checkAccountExists(oldUserId)
      if (!accountExists) {
        return@withContext false
      }

      val transactionResult = restoreAccountRepository.cleanDatabase(oldUserId)
      if (!transactionResult) {
        throw DatabaseException("Could not clean database")
      }

      return@withContext true
    }
  }

}