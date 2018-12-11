package com.kirakishou.photoexchange.mvp.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.interactors.RestoreAccountUseCase
import com.kirakishou.photoexchange.mvp.viewmodel.SettingsActivityViewModel
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