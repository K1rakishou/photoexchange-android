package com.kirakishou.photoexchange.di.module.activity

import androidx.lifecycle.ViewModelProviders
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.mvrx.viewmodel.ViewTakenPhotoActivityViewModel
import com.kirakishou.photoexchange.mvrx.viewmodel.factory.ViewTakenPhotoActivityViewModelFactory
import com.kirakishou.photoexchange.ui.activity.ViewTakenPhotoActivity
import dagger.Module
import dagger.Provides

/**
 * Created by kirakishou on 3/9/2018.
 */

@Module
open class ViewTakenPhotoActivityModule(
  val activity: ViewTakenPhotoActivity
) {

  @PerActivity
  @Provides
  open fun provideViewModelFactory(takenPhotosRepository: TakenPhotosRepository,
                                   settingsRepository: SettingsRepository,
                                   dispatchersProvider: DispatchersProvider): ViewTakenPhotoActivityViewModelFactory {
    return ViewTakenPhotoActivityViewModelFactory(
      takenPhotosRepository,
      settingsRepository,
      dispatchersProvider
    )
  }

  @PerActivity
  @Provides
  fun provideViewModel(
    viewModelFactory: ViewTakenPhotoActivityViewModelFactory
  ): ViewTakenPhotoActivityViewModel {
    return ViewModelProviders.of(
      activity,
      viewModelFactory
    ).get(ViewTakenPhotoActivityViewModel::class.java)
  }
}