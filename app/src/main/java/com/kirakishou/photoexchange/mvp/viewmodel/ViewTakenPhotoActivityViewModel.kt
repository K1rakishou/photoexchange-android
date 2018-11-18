package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.ui.fragment.AddToGalleryDialogFragment
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

/**
 * Created by kirakishou on 3/9/2018.
 */
class ViewTakenPhotoActivityViewModel(
  private val schedulerProvider: SchedulerProvider,
  private val takenPhotosRepository: TakenPhotosRepository,
  private val settingsRepository: SettingsRepository
) : BaseViewModel() {

  private val TAG = "ViewTakenPhotoActivityViewModel"

  val addToGalleryFragmentResult = PublishSubject.create<AddToGalleryDialogFragment.FragmentResult>().toSerialized()

  override fun onCleared() {
    Timber.tag(TAG).d("onCleared()")

    super.onCleared()
  }

  suspend fun queueUpTakenPhoto(takenPhotoId: Long): Boolean {
    return takenPhotosRepository.updatePhotoState(takenPhotoId, PhotoState.PHOTO_QUEUED_UP)
  }

  suspend fun updateSetIsPhotoPublic(takenPhotoId: Long): Boolean {
    return takenPhotosRepository.updateMakePhotoPublic(takenPhotoId)
  }

  suspend fun saveMakePublicFlag(rememberChoice: Boolean, makePublic: Boolean) {
    if (!rememberChoice) {
      return
    }

    settingsRepository.saveMakePublicFlag(makePublic)
  }

  suspend fun getMakePublicFlag():SettingsRepository.MakePhotosPublicState {
    return settingsRepository.getMakePublicFlag()
  }
}