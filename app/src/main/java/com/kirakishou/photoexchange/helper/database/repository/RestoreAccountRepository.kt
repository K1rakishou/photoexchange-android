package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import kotlinx.coroutines.withContext

class RestoreAccountRepository(
  private val database: MyDatabase,
  private val settingsRepository: SettingsRepository,
  private val uploadedPhotosRepository: UploadedPhotosRepository,
  private val receivedPhotosRepository: ReceivedPhotosRepository,
  dispatchersProvider: DispatchersProvider
) : BaseRepository(dispatchersProvider) {

  suspend fun cleanDatabase(oldUserId: String): Boolean {
    return withContext(coroutineContext) {
      return@withContext database.transactional {
        if (!settingsRepository.saveUserId(oldUserId)) {
          return@transactional false
        }

        uploadedPhotosRepository.deleteAll()
        receivedPhotosRepository.deleteAll()

        return@transactional true
      }
    }
  }
}