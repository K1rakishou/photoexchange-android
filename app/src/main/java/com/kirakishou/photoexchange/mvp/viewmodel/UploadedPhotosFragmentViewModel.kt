package com.kirakishou.photoexchange.mvp.viewmodel

import androidx.fragment.app.FragmentActivity
import com.airbnb.mvrx.BaseMvRxViewModel
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.interactors.GetUploadedPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
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
    when (takenPhotosRepository.figureOutWhatPhotosToLoad()) {
      TakenPhotosRepository.PhotosToLoad.QueuedUpAndFailed -> {
        Timber.tag(TAG).d("Loading queued up and failed photos")
        loadQueuedUpAndFailedPhotos()
      }
      TakenPhotosRepository.PhotosToLoad.Uploaded -> {
        Timber.tag(TAG).d("Loading uploaded photos")
        loadUploadedPhotos(photosPerPage)
      }
    }
  }

  private fun loadQueuedUpAndFailedPhotos() {
    withState { state ->
      if (state.takenPhotosRequest is Loading) {
        return@withState
      }

      val singleResult = rxSingle {
        loadQueuedUpAndFailedPhotosInternal()
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

  private suspend fun loadQueuedUpAndFailedPhotosInternal(): List<TakenPhoto> {
    takenPhotosRepository.resetStalledPhotosState()

    return mutableListOf<TakenPhoto>().apply {
      addAll(takenPhotosRepository.findAllByState(PhotoState.PHOTO_QUEUED_UP))
      addAll(takenPhotosRepository.findAllByState(PhotoState.FAILED_TO_UPLOAD))
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
        //TODO
//        adapter.addTakenPhoto(event.photo)
//        adapter.updatePhotoProgress(event.photo.id, event.progress)
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
        //TODO
//        adapter.removePhotoById(event.photo.id)
//
//        val photo = event.photo
//          .also { it.photoState = PhotoState.FAILED_TO_UPLOAD }
//        adapter.addTakenPhoto(photo)
      }
      is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnEnd -> {
        //TODO
//          triggerPhotosLoading()
      }
      is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnError -> {
        //TODO
//          handleUnknownErrors(event.exception)
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