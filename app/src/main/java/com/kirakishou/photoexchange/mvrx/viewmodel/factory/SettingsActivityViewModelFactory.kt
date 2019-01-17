package com.kirakishou.photoexchange.mvrx.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.usecases.RestoreAccountUseCase
import com.kirakishou.photoexchange.mvrx.viewmodel.SettingsActivityViewModel
import javax.inject.Inject

class SettingsActivityViewModelFactory
@Inject constructor(
  val settingsRepository: SettingsRepository,
  val restoreAccountUseCase: RestoreAccountUseCase,
  val dispatchersProvider: DispatchersProvider
) : ViewModelProvider.Factory {

  override fun <T : ViewModel?> create(modelClass: Class<T>): T {
    return SettingsActivityViewModel(
      settingsRepository,
      restoreAccountUseCase,
      dispatchersProvider
    ) as T
  }

}