package com.kirakishou.photoexchange.mvp.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.mvp.viewmodel.ViewTakenPhotoActivityViewModel
import javax.inject.Inject

/**
 * Created by kirakishou on 3/9/2018.
 */
class ViewTakenPhotoActivityViewModelFactory
@Inject constructor(
  val takenPhotosRepository: TakenPhotosRepository,
  val settingsRepository: SettingsRepository,
  val dispatchersProvider: DispatchersProvider
) : ViewModelProvider.Factory {

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel?> create(modelClass: Class<T>): T {
    return ViewTakenPhotoActivityViewModel(
      takenPhotosRepository,
      settingsRepository,
      dispatchersProvider
    ) as T
  }
}