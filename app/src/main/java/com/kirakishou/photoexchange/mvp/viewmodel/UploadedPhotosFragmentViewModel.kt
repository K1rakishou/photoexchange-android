package com.kirakishou.photoexchange.mvp.viewmodel

import androidx.fragment.app.FragmentActivity
import com.airbnb.mvrx.*
import com.kirakishou.fixmypc.photoexchange.BuildConfig
import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.PhotoSize
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.interactors.GetUploadedPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.photo.QueuedUpPhoto
import com.kirakishou.photoexchange.mvp.model.photo.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.photo.UploadingPhoto
import com.kirakishou.photoexchange.mvp.viewmodel.state.UploadedPhotosFragmentState
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.fragment.UploadedPhotosFragment
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

class UploadedPhotosFragmentViewModel(
  initialState: UploadedPhotosFragmentState,
  private val takenPhotosRepository: TakenPhotosRepository,
  private val settingsRepository: SettingsRepository,
  private val getUploadedPhotosUseCase: GetUploadedPhotosUseCase,
  private val dispatchersProvider: DispatchersProvider
) : BaseMvRxViewModel<UploadedPhotosFragmentState>(initialState, BuildConfig.DEBUG), CoroutineScope {
  private val TAG = "UploadedPhotosFragmentViewModel"

  private val compositeDisposable = CompositeDisposable()
  private val job = Job()
  private val photosPerPage = 10

  lateinit var photoSize: PhotoSize
  lateinit var intercom: PhotosActivityViewModelIntercom

  override val coroutineContext: CoroutineContext
    get() = job + dispatchersProvider.GENERAL()

  init {
    launch { loadQueuedUpPhotos() }
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

    launch { loadQueuedUpPhotos() }
  }

  fun cancelPhotoUploading(photoId: Long) {
    launch {
      if (takenPhotosRepository.findById(photoId) == null) {
        return@launch
      }

      takenPhotosRepository.deletePhotoById(photoId)

      intercom.tell<PhotosActivity>()
        .to(PhotosActivityEvent.CancelPhotoUploading(photoId))

      withState { state ->
        val newPhotos = state.takenPhotos.toMutableList()
        newPhotos.removeAll { it.id == photoId }

        setState { copy(takenPhotos = newPhotos) }
        invalidate()
      }
    }
  }

  private fun loadQueuedUpPhotos() {
    withState { state ->
      if (state.takenPhotosRequest is Loading) {
        return@withState
      }

      launch {
        setState { copy(takenPhotosRequest = Loading()) }

        val request = try {
          Success(takenPhotosRepository.loadNotUploadedPhotos())
        } catch (error: Throwable) {
          Fail<List<TakenPhoto>>(error)
        }

        setState {
          copy(
            takenPhotosRequest = request,
            takenPhotos = state.takenPhotos + (request() ?: emptyList())
          )
        }

        invalidate()
        loadUploadedPhotos(10)
      }
    }
  }

  private fun loadUploadedPhotos(count: Int) {
    withState { state ->
      if (state.uploadedPhotosRequest is Loading) {
        return@withState
      }

      launch {
        val userId = settingsRepository.getUserId()
        val lastUploadedOn = state.uploadedPhotos
          .lastOrNull()
          ?.uploadedOn
          ?: -1L

        setState { copy(uploadedPhotosRequest = Loading()) }

        val request = try {
          Success(loadPageOfUploadedPhotos(userId, lastUploadedOn, count))
        } catch (error: Throwable) {
          Fail<List<UploadedPhoto>>(error)
        }

        setState {
          copy(
            uploadedPhotosRequest = request,
            uploadedPhotos = state.uploadedPhotos + (request() ?: emptyList())
          )
        }

        invalidate()
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
        return result.value.also { uploadedPhotos ->
          uploadedPhotos.forEach { uploadedPhoto -> uploadedPhoto.photoSize = photoSize }
        }
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
        Timber.tag(TAG).d("OnPhotoUploadingStart")

        withState { state ->
          val photoIndex = state.takenPhotos.indexOfFirst { it.id == event.photo.id }
          if (photoIndex != -1) {
            val filteredPhotos = state.takenPhotos
              .filter { it.id != event.photo.id }
              .toMutableList()

            filteredPhotos += UploadingPhoto.fromMyPhoto(state.takenPhotos[photoIndex], 0)
            setState { copy(takenPhotos = filteredPhotos) }
          } else {
            val newPhoto = UploadingPhoto.fromMyPhoto(event.photo, 0)
            setState { copy(takenPhotos = state.takenPhotos + newPhoto) }
          }

          invalidate()
        }
      }
      is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress -> {
        Timber.tag(TAG).d("OnPhotoUploadingProgress")

        withState { state ->
          val photoIndex = state.takenPhotos.indexOfFirst { it.id == event.photo.id }
          val newPhotos = if (photoIndex != -1) {
            state.takenPhotos.filter { it.id != event.photo.id }.toMutableList()
          } else {
            state.takenPhotos.toMutableList()
          }

          newPhotos.add(photoIndex, UploadingPhoto.fromMyPhoto(event.photo, event.progress))
          setState { copy(takenPhotos = newPhotos) }
          invalidate()
        }
      }
      is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploaded -> {
        Timber.tag(TAG).d("OnPhotoUploaded")

        withState { state ->
          val photoIndex = state.takenPhotos.indexOfFirst { it.id == event.photo.id }
          if (photoIndex == -1) {
            return@withState
          }

          val newPhotos = state.takenPhotos.filter { it.id != event.photo.id }
          setState { copy(takenPhotos = newPhotos) }
          invalidate()
        }
      }
      is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnFailedToUploadPhoto -> {
        withState { state ->
          val photoIndex = state.takenPhotos.indexOfFirst { it.id == event.photo.id }
          val newPhotos = if (photoIndex != -1) {
            state.takenPhotos.filter { it.id != event.photo.id }.toMutableList()
          } else {
            state.takenPhotos.toMutableList()
          }

          newPhotos.add(photoIndex, QueuedUpPhoto.fromTakenPhoto(event.photo))
          setState { copy(takenPhotos = newPhotos) }
          invalidate()
        }
      }
      is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoCanceled -> {
        cancelPhotoUploading(event.photo.id)
      }
      is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnError -> {
        withState { state ->
          val newPhotos = state.takenPhotos
            .map { takenPhoto -> QueuedUpPhoto.fromTakenPhoto(takenPhoto) }

          setState { copy(takenPhotos = newPhotos) }
          invalidate()
        }
      }
      is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnEnd -> {
        Timber.tag(TAG).d("OnEnd")

        loadUploadedPhotos(photosPerPage)
      }
    }.safe
  }

  private fun invalidate() {
    intercom.tell<UploadedPhotosFragment>()
      .to(UploadedPhotosFragmentEvent.GeneralEvents.Invalidate)
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