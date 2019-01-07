package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.source.remote.FirebaseRemoteSource
import core.SharedConstants
import kotlinx.coroutines.withContext

open class CheckFirebaseAvailabilityUseCase(
  private val firebaseRemoteSource: FirebaseRemoteSource,
  private val settingsRepository: SettingsRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {

  suspend fun check(): FirebaseAvailabilityResult {
    return withContext(coroutineContext) {
      if (settingsRepository.isNoFirebaseDialogAlreadyShown()) {
        return@withContext FirebaseAvailabilityResult.AlreadyShown
      }

      try {
        val token = firebaseRemoteSource.getTokenAsync()

        try {
          return@withContext if (token == SharedConstants.NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN) {
            FirebaseAvailabilityResult.NotAvailable
          } else {
            FirebaseAvailabilityResult.Available
          }
        } finally {
          settingsRepository.setNoFirebaseDialogAlreadyShown()
        }
      } catch (error: Throwable) {
        return@withContext FirebaseAvailabilityResult.NotAvailable
      }
    }
  }

  enum class FirebaseAvailabilityResult {
    AlreadyShown,
    NotAvailable,
    Available
  }
}