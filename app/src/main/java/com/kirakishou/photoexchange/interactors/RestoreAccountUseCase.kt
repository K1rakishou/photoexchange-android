package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.RestoreAccountRepository
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.helper.myRunCatching
import kotlinx.coroutines.withContext

open class RestoreAccountUseCase(
  private val apiClient: ApiClient,
  private val restoreAccountRepository: RestoreAccountRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {

  suspend fun restoreAccount(oldUserId: String): Either<Exception, Boolean> {
    return withContext(coroutineContext) {
      return@withContext myRunCatching {
        val accountExists = apiClient.checkAccountExists(oldUserId)
        if (!accountExists) {
          return@myRunCatching false
        }

        val transactionResult = restoreAccountRepository.cleanDatabase(oldUserId)
        if (!transactionResult) {
          throw DatabaseException("Could not clean database")
        }

        return@myRunCatching true
      }
    }
  }

}