package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.interactors.RestoreAccountUseCase
import com.kirakishou.photoexchange.mvp.model.other.Constants.DOMAIN_NAME

class SettingsActivityViewModel(
  private val settingsRepository: SettingsRepository,
  private val schedulerProvider: SchedulerProvider,
  private val restoreAccountUseCase: RestoreAccountUseCase
) : BaseViewModel() {
  private val TAG = "SettingsActivityViewModel"


  suspend fun resetMakePublicPhotoOption() {
    settingsRepository.saveMakePublicFlag(null)
  }

  suspend fun getUserId(): String {
    return settingsRepository.getUserId()
  }

  suspend fun restoreOldAccount(oldUserId: String): Boolean {
    val suffix = "@${DOMAIN_NAME}"

    val userId = if (!oldUserId.endsWith(suffix, true)) {
      oldUserId + suffix
    } else {
      oldUserId
    }

    val result = restoreAccountUseCase.restoreAccount(userId)

    when (result) {
      is Either.Value -> return result.value
      is Either.Error -> throw result.error
    }.safe
  }
}