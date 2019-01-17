package com.kirakishou.photoexchange.mvrx.viewmodel

import com.kirakishou.photoexchange.helper.PhotosVisibility
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.mvrx.model.PhotoState
import com.kirakishou.photoexchange.ui.fragment.AddToGalleryDialogFragment
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.withContext

/**
 * Created by kirakishou on 3/9/2018.
 */
class ViewTakenPhotoActivityViewModel(
  private val takenPhotosRepository: TakenPhotosRepository,
  private val settingsRepository: SettingsRepository,
  dispatchersProvider: DispatchersProvider
) : BaseViewModel(dispatchersProvider) {

  private val TAG = "ViewTakenPhotoActivityViewModel"

  val addToGalleryFragmentResult = PublishSubject.create<AddToGalleryDialogFragment.FragmentResult>().toSerialized()

  override fun onCleared() {
    super.onCleared()
  }

  suspend fun queueUpTakenPhoto(takenPhotoId: Long): Boolean {
    return withContext(coroutineContext) {
      takenPhotosRepository.updatePhotoState(takenPhotoId, PhotoState.PHOTO_QUEUED_UP)
    }
  }

  suspend fun updateSetIsPhotoPublic(takenPhotoId: Long, makePublic: Boolean): Boolean {
    return withContext(coroutineContext) {
      takenPhotosRepository.updateMakePhotoPublic(takenPhotoId, makePublic)
    }
  }

  suspend fun saveMakePublicFlag(rememberChoice: Boolean, makePublic: Boolean) {
    return withContext(coroutineContext) {
      if (!rememberChoice) {
        return@withContext
      }

      settingsRepository.savePhotoVisibility(makePublic)
    }
  }

  suspend fun getMakePublicFlag(): PhotosVisibility {
    return withContext(coroutineContext) {
      settingsRepository.getPhotoVisibility()
    }
  }
}