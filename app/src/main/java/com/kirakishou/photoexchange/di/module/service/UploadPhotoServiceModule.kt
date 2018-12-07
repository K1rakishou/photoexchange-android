package com.kirakishou.photoexchange.di.module.service

import com.kirakishou.photoexchange.di.scope.PerService
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.interactors.GetUserIdUseCase
import com.kirakishou.photoexchange.interactors.UpdateFirebaseTokenUseCase
import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
import com.kirakishou.photoexchange.service.UploadPhotoServicePresenter
import dagger.Module
import dagger.Provides

/**
 * Created by kirakishou on 3/17/2018.
 */

@Module
class UploadPhotoServiceModule {

  @PerService
  @Provides
  fun provideUploadPhotoServicePresenter(settingsRepository: SettingsRepository,
                                         takenPhotosRepository: TakenPhotosRepository,
                                         uploadPhotosUseCase: UploadPhotosUseCase,
                                         getUserIdUseCase: GetUserIdUseCase,
                                         updateFirebaseTokenUseCase: UpdateFirebaseTokenUseCase,
                                         dispatchersProvider: DispatchersProvider): UploadPhotoServicePresenter {
    return UploadPhotoServicePresenter(
      settingsRepository,
      takenPhotosRepository,
      uploadPhotosUseCase,
      getUserIdUseCase,
      updateFirebaseTokenUseCase,
      dispatchersProvider
    )
  }
}