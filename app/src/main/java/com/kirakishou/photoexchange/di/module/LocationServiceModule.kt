package com.kirakishou.photoexchange.di.module

import android.content.Context
import com.kirakishou.photoexchange.di.scope.PerService
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.location.LocationService
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
class LocationServiceModule {

  @PerService
  @Provides
  fun provideLocationService(@Named("app_context") context: Context,
                             takenPhotosRepository: TakenPhotosRepository,
                             settingsRepository: SettingsRepository): LocationService {
    return LocationService(context, takenPhotosRepository, settingsRepository)
  }
}