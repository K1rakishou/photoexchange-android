package com.kirakishou.photoexchange.mvp.viewmodel

import androidx.fragment.app.FragmentActivity
import com.airbnb.mvrx.BaseMvRxViewModel
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.interactors.GetUploadedPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.photo.QueuedUpPhoto
import com.kirakishou.photoexchange.mvp.model.photo.UploadingPhoto
import com.kirakishou.photoexchange.mvp.viewmodel.state.UploadedPhotosFragmentState
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.rxSingle
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

class UploadedPhotosFragmentViewModel(
  initialState: UploadedPhotosFragmentState,
  private val takenPhotosRepository: TakenPhotosRepository,
  private val settingsRepository: SettingsRepository,
  private val getUploadedPhotosUseCase: GetUploadedPhotosUseCase,
  private val dispatchersProvider: DispatchersProvider
) : BaseMvRxViewModel<UploadedPhotosFragmentState>(initialState), CoroutineScope {
  private val TAG = "UploadedPhotosFragmentViewModel"

  private val compositeDisposable = CompositeDisposable()
  private val job = Job()
  private val photosPerPage = 10

  lateinit var intercom: PhotosActivityViewModelIntercom

  override val coroutineContext: CoroutineContext
    get() = job + dispatchersProvider.GENERAL()

  init {
    launch { loadPhotos() }
  }

  private suspend fun loadPhotos() {
    //TODO
//    when (takenPhotosRepository.figureOutWhatPhotosToLoad()) {
//      TakenPhotosRepository.PhotosToLoad.QueuedUpAndFailed -> {
//        Timber.tag(TAG).d("Loading queued up and failed photos")
//        loadNotUploadedPhotos()
//      }
//      TakenPhotosRepository.PhotosToLoad.Uploaded -> {
//        Timber.tag(TAG).d("Loading uploaded photos")
//        loadUploadedPhotos(photosPerPage)
//      }
//    }
  }

  fun resetState() {
    setState {
      copy(
        takenPhotos = emptyList(),
        takenPhotosRequest = Uninitialized,
        uploadedPhotos = emptyList(),
        uploadedPhotosRequest = Uninitialized
      )
    }

    launch { loadPhotos() }
  }

  private fun loadNotUploadedPhotos() {
    withState { state ->
      if (state.takenPhotosRequest is Loading) {
        return@withState
      }

      val singleResult = rxSingle {
        takenPhotosRepository.loadNotUploadedPhotos()
      }

      singleResult
        .execute { request ->
          copy(
            takenPhotosRequest = request,
            takenPhotos = state.takenPhotos + (request() ?: emptyList())
          )
        }
        .disposeOnClear()
    }
  }

  private fun loadUploadedPhotos(count: Int) {
    withState { state ->
      if (state.takenPhotosRequest is Loading) {
        return@withState
      }

      val singleResult = rxSingle {
        val userId = settingsRepository.getUserId()
        val lastUploadedOn = state.uploadedPhotos
          .lastOrNull()
          ?.uploadedOn
          ?: -1L

        loadPageOfUploadedPhotos(userId, lastUploadedOn, count)
      }

      singleResult
        .execute { request ->
          copy(
            uploadedPhotosRequest = request,
            uploadedPhotos = state.uploadedPhotos + (request() ?: emptyList())
          )
        }
    }
  }

  private suspend fun loadPageOfUploadedPhotos(
    userId: String,
    lastUploadedOn: Long,
    count: Int
  ): List<UploadedPhoto> {
    if (userId.isEmpty()) {
      return emptyList()
    }

    val result = getUploadedPhotosUseCase.loadPageOfPhotos(userId, lastUploadedOn, count)
    when (result) {
      is Either.Value -> {
        return result.value
      }
      is Either.Error -> {
        throw result.error
      }
    }
  }

  fun clear() {
    onCleared()
  }

  fun onUploadingEvent(event: UploadedPhotosFragmentEvent.PhotoUploadEvent) {
    when (event) {
      is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingStart -> {
        withState { state ->
          val photoIndex = state.takenPhotos.indexOfFirst { it.id == event.photo.id }
          if (photoIndex != -1) {
            val updatePhotos = state.takenPhotos.toMutableList()
            updatePhotos[photoIndex]
              .also { it.photoState = PhotoState.PHOTO_UPLOADING }

            setState { copy(takenPhotos = updatePhotos) }
          } else {
            val newPhoto = event.photo
              .also { it.photoState = PhotoState.PHOTO_UPLOADING }

            setState { copy(takenPhotos = state.takenPhotos + newPhoto) }
          }
        }
      }
      is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress -> {
        withState { state ->
          val photoIndex = state.takenPhotos.indexOfFirst { it.id == event.photo.id }
          val newPhotos = if (photoIndex != -1) {
            state.takenPhotos.filter { it.id == event.photo.id }.toMutableList()
          } else {
            state.takenPhotos.toMutableList()
          }

          newPhotos.add(photoIndex, UploadingPhoto.fromMyPhoto(event.photo, event.progress))
          setState { copy(takenPhotos = newPhotos) }
        }
      }
      is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploaded -> {
        withState { state ->
          val photoIndex = state.takenPhotos.indexOfFirst { it.id == event.photo.id }
          if (photoIndex == -1) {
            return@withState
          }

          val newPhotos = state.takenPhotos.filter { it.id == event.photo.id }
          setState { copy(takenPhotos = newPhotos) }
        }
      }
      is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnFailedToUploadPhoto -> {
        withState { state ->
          val photoIndex = state.takenPhotos.indexOfFirst { it.id == event.photo.id }
          val newPhotos = if (photoIndex != -1) {
            state.takenPhotos.filter { it.id == event.photo.id }.toMutableList()
          } else {
            state.takenPhotos.toMutableList()
          }

          newPhotos.add(photoIndex, QueuedUpPhoto.fromTakenPhoto(event.photo))
          setState { copy(takenPhotos = newPhotos) }
        }
      }
      is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnError -> {
        withState { state ->
          val newPhotos = state.takenPhotos
            .map { takenPhoto -> QueuedUpPhoto.fromTakenPhoto(takenPhoto) }

          setState { copy(takenPhotos = newPhotos) }
        }
      }
      is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnEnd -> {
        //TODO
//          triggerPhotosLoading()
      }
    }.safe
  }

  override fun onCleared() {
    super.onCleared()
    Timber.tag(TAG).d("onCleared()")

    compositeDisposable.dispose()
    job.cancel()
  }

  companion object : MvRxViewModelFactory<UploadedPhotosFragmentState> {
    override fun create(
      activity: FragmentActivity,
      state: UploadedPhotosFragmentState
    ): BaseMvRxViewModel<UploadedPhotosFragmentState> {
      return (activity as PhotosActivity).viewModel.uploadedPhotosFragmentViewModel
    }
  }
}