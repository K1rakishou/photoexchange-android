package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.helper.exception.NetworkAccessDisabledInSettings
import com.kirakishou.photoexchange.helper.util.NetUtils
import kotlinx.coroutines.withContext

open class RestoreAccountUseCase(
  private val apiClient: ApiClient,
  private val database: MyDatabase,
  private val netUtils: NetUtils,
  private val settingsRepository: SettingsRepository,
  private val uploadedPhotosRepository: UploadedPhotosRepository,
  private val receivedPhotosRepository: ReceivedPhotosRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {

  suspend fun restoreAccount(oldUserId: String): Boolean {
    return withContext(coroutineContext) {
      if (!netUtils.canAccessNetwork()) {
        throw NetworkAccessDisabledInSettings()
      }

      val accountExists = apiClient.checkAccountExists(oldUserId)
      if (!accountExists) {
        return@withContext false
      }

      val transactionResult = cleanDatabase(oldUserId)
      if (!transactionResult) {
        throw DatabaseException("Could not clean database")
      }

      return@withContext true
    }
  }

  private suspend fun cleanDatabase(oldUserId: String): Boolean {
    return database.transactional {
      if (!settingsRepository.saveUserUuid(oldUserId)) {
        return@transactional false
      }

      uploadedPhotosRepository.deleteAll()
      receivedPhotosRepository.deleteAll()

      return@transactional true
    }
  }

}