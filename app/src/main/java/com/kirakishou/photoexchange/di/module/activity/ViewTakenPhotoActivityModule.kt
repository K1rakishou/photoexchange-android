package com.kirakishou.photoexchange.di.module.activity

import androidx.lifecycle.ViewModelProviders
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.mvp.viewmodel.ViewTakenPhotoActivityViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.factory.ViewTakenPhotoActivityViewModelFactory
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
  open fun provideViewModelFactory(schedulerProvider: SchedulerProvider,
                                   takenPhotosRepository: TakenPhotosRepository,
                                   settingsRepository: SettingsRepository): ViewTakenPhotoActivityViewModelFactory {
    return ViewTakenPhotoActivityViewModelFactory(schedulerProvider, takenPhotosRepository, settingsRepository)
  }

  @PerActivity
  @Provides
  fun provideViewModel(viewModelFactory: ViewTakenPhotoActivityViewModelFactory): ViewTakenPhotoActivityViewModel {
    return ViewModelProviders.of(activity, viewModelFactory).get(ViewTakenPhotoActivityViewModel::class.java)
  }
}