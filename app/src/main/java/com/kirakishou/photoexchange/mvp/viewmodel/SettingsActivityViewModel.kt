package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.Constants.DOMAIN_NAME
import com.kirakishou.photoexchange.helper.NetworkAccessLevel
import com.kirakishou.photoexchange.helper.PhotosVisibility
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.interactors.RestoreAccountUseCase
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class SettingsActivityViewModel(
  private val settingsRepository: SettingsRepository,
  private val restoreAccountUseCase: RestoreAccountUseCase,
  dispatchersProvider: DispatchersProvider
) : BaseViewModel(dispatchersProvider) {
  private val TAG = "SettingsActivityViewModel"

  suspend fun getUserId(): String {
    return settingsRepository.getUserId()
  }

  fun updatePhotoVisibility(visibility: PhotosVisibility) {
    launch {
      val makePublic = when (visibility) {
        PhotosVisibility.AlwaysPublic -> true
        PhotosVisibility.AlwaysPrivate -> false
        PhotosVisibility.Neither -> null
      }

      if (!settingsRepository.savePhotoVisibility(makePublic)) {
        throw DatabaseException("Could not update PhotosVisibility, visibility = ${visibility}")
      }
    }
  }

  suspend fun getPhotoVisibility(): PhotosVisibility {
    return withContext(coroutineContext) {
      return@withContext settingsRepository.getPhotoVisibility().also {
        Timber.tag(TAG).d("Current photo visibility = ${it}")
      }
    }
  }

  fun updateNetworkAccessLevel(level: NetworkAccessLevel) {
    launch {
      if (!settingsRepository.setNetworkAccessLevel(level)) {
        throw DatabaseException("Could not update NetworkAccessLevel, level = ${level}")
      }
    }
  }

  suspend fun getNetworkAccessLevel(): NetworkAccessLevel {
    return withContext(coroutineContext) {
      return@withContext settingsRepository.getNetworkAccessLevel().also {
        Timber.tag(TAG).d("Current network access level = ${it}")
      }
    }
  }

  suspend fun restoreOldAccount(oldUserId: String): Boolean {
    return withContext(coroutineContext) {
      val suffix = "@${DOMAIN_NAME}"

      val userId = if (!oldUserId.endsWith(suffix, true)) {
        oldUserId + suffix
      } else {
        oldUserId
      }

      return@withContext restoreAccountUseCase.restoreAccount(userId)
    }
  }
}