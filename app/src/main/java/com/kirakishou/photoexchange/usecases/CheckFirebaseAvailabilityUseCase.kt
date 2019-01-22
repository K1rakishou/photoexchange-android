package com.kirakishou.photoexchange.usecases

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.source.remote.FirebaseRemoteSource
import core.SharedConstants
import kotlinx.coroutines.withContext
import timber.log.Timber

open class CheckFirebaseAvailabilityUseCase(
  private val firebaseRemoteSource: FirebaseRemoteSource,
  private val settingsRepository: SettingsRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "CheckFirebaseAvailabilityUseCase"

  suspend fun check(): FirebaseAvailabilityResult {
    return withContext(coroutineContext) {
      if (settingsRepository.isNoFirebaseDialogAlreadyShown()) {
        Timber.tag(TAG).d("NoFirebaseDialog has already been shown")
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
        Timber.tag(TAG).d(error, "Error while trying to get firebase token")
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