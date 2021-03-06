package com.kirakishou.photoexchange.di.module.activity

import androidx.lifecycle.ViewModelProviders
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.CameraProvider
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.permission.PermissionManager
import com.kirakishou.photoexchange.usecases.TakePhotoUseCase
import com.kirakishou.photoexchange.mvrx.viewmodel.TakePhotoActivityViewModel
import com.kirakishou.photoexchange.mvrx.viewmodel.factory.TakePhotoActivityViewModelFactory
import com.kirakishou.photoexchange.ui.activity.TakePhotoActivity
import dagger.Module
import dagger.Provides

/**
 * Created by kirakishou on 3/3/2018.
 */

@Module
open class TakePhotoActivityModule(
  val activity: TakePhotoActivity
) {

  @PerActivity
  @Provides
  open fun provideViewModelFactory(settingsRepository: SettingsRepository,
                                   dispatchersProvider: DispatchersProvider): TakePhotoActivityViewModelFactory {
    return TakePhotoActivityViewModelFactory(
      settingsRepository,
      dispatchersProvider
    )
  }

  @PerActivity
  @Provides
  open fun provideViewModel(viewModelFactory: TakePhotoActivityViewModelFactory): TakePhotoActivityViewModel {
    return ViewModelProviders.of(
      activity,
      viewModelFactory
    ).get(TakePhotoActivityViewModel::class.java)
  }

  @PerActivity
  @Provides
  open fun provideCameraProvider(takenPhotosRepository: TakenPhotosRepository,
                                 takePhotoUseCase: TakePhotoUseCase): CameraProvider {
    return CameraProvider(
      activity,
      takePhotoUseCase
    )
  }

  @PerActivity
  @Provides
  open fun providePermissionManager(): PermissionManager {
    return PermissionManager()
  }
}