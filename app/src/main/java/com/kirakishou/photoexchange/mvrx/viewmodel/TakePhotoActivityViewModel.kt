package com.kirakishou.photoexchange.mvrx.viewmodel

import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository

/**
 * Created by kirakishou on 11/7/2017.
 */
class TakePhotoActivityViewModel(
  private val settingsRepository: SettingsRepository,
  dispatchersProvider: DispatchersProvider
) : BaseViewModel(dispatchersProvider) {

  private val TAG = "TakePhotoActivityViewModel"

  override fun onCleared() {
    super.onCleared()
  }

}
















