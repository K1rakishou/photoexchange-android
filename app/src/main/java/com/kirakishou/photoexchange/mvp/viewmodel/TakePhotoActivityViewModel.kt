package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import core.ErrorCode
import io.reactivex.subjects.PublishSubject

/**
 * Created by kirakishou on 11/7/2017.
 */
class TakePhotoActivityViewModel(
  private val schedulerProvider: SchedulerProvider,
  private val takenPhotosRepository: TakenPhotosRepository,
  private val settingsRepository: SettingsRepository
) : BaseViewModel() {

  private val TAG = "TakePhotoActivityViewModel"

  val errorCodesSubject = PublishSubject.create<ErrorCode>().toSerialized()

  override fun onCleared() {
    super.onCleared()
  }

  suspend fun updateGpsPermissionGranted(granted: Boolean) {
    settingsRepository.updateGpsPermissionGranted(granted)
  }
}
















