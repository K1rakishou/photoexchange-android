package com.kirakishou.photoexchange.di.module.activity

import androidx.lifecycle.ViewModelProviders
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.interactors.RestoreAccountUseCase
import com.kirakishou.photoexchange.mvp.viewmodel.SettingsActivityViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.factory.SettingsActivityViewModelFactory
import com.kirakishou.photoexchange.ui.activity.SettingsActivity
import dagger.Module
import dagger.Provides

@Module
open class SettingsActivityModule(
  val activity: SettingsActivity
) {

  @PerActivity
  @Provides
  fun provideViewModelFactory(settingsRepository: SettingsRepository,
                              restoreAccountUseCase: RestoreAccountUseCase,
                              dispatchersProvider: DispatchersProvider): SettingsActivityViewModelFactory {
    return SettingsActivityViewModelFactory(
      settingsRepository,
      restoreAccountUseCase,
      dispatchersProvider
    )
  }

  @PerActivity
  @Provides
  fun provideViewModel(viewModelFactory: SettingsActivityViewModelFactory): SettingsActivityViewModel {
    return ViewModelProviders.of(
      activity,
      viewModelFactory
    ).get(SettingsActivityViewModel::class.java)
  }
}