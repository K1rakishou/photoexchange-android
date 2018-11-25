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
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.model.photo.QueuedUpPhoto
import com.kirakishou.photoexchange.mvp.model.photo.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.photo.UploadingPhoto
import com.kirakishou.photoexchange.mvp.viewmodel.state.UploadedPhotosFragmentState
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.fragment.UploadedPhotosFragment
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

class UploadedPhotosFragmentViewModel(
  initialState: UploadedPhotosFragmentState,
  private val intercom: PhotosActivityViewModelIntercom,
  private val takenPhotosRepository: TakenPhotosRepository,
  private val settingsRepository: SettingsRepository,
  private val getUploadedPhotosUseCase: GetUploadedPhotosUseCase,
  private val dispatchersProvider: DispatchersProvider
) : BaseMvRxViewModel<UploadedPhotosFragmentState>(initialState, BuildConfig.DEBUG), CoroutineScope {
  private val TAG = "UploadedPhotosFragmentViewModel"

  private val compositeDisposable = CompositeDisposable()
  private val job = Job()
  private val viewModelActor: SendChannel<ActorAction>

  var photosPerPage: Int = Constants.DEFAULT_PHOTOS_PER_PAGE_COUNT
  var photoSize: PhotoSize = PhotoSize.Medium

  override val coroutineContext: CoroutineContext
    get() = job + dispatchersProvider.GENERAL()

  init {
    viewModelActor = actor(capacity = Channel.UNLIMITED) {
      consumeEach { action ->
        if (!isActive) {
          return@consumeEach
        }

        when (action) {
          ActorAction.ResetState -> resetStateInternal()
          is ActorAction.CancelPhotoUploading -> cancelPhotoUploadingInternal(action.photoId)
          ActorAction.LoadQueuedUpPhotos -> loadQueuedUpPhotosInternal()
          ActorAction.LoadUploadedPhotos -> loadUploadedPhotosInternal()
        }.safe
      }
    }

    loadQueuedUpPhotos()
  }

  fun resetState() {
    launch { viewModelActor.send(ActorAction.ResetState) }
  }

  fun cancelPhotoUploading(photoId: Long) {
    launch { viewModelActor.send(ActorAction.CancelPhotoUploading(photoId)) }
  }

  fun loadQueuedUpPhotos() {
    launch { viewModelActor.send(ActorAction.LoadQueuedUpPhotos) }
  }

  fun loadUploadedPhotos() {
    launch { viewModelActor.send(ActorAction.LoadUploadedPhotos) }
  }

  private fun resetStateInternal() {
    setState { UploadedPhotosFragmentState() }
    launch { loadQueuedUpPhotos() }
  }

  private fun cancelPhotoUploadingInternal(photoId: Long) {
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
      }
    }
  }

  private fun loadQueuedUpPhotosInternal() {
    withState { state ->
      launch {
        val request = try {
          Success(takenPhotosRepository.loadNotUploadedPhotos())
        } catch (error: Throwable) {
          Fail<List<TakenPhoto>>(error)
        }

        setState {
          copy(
            takenPhotos = state.takenPhotos + (request() ?: emptyList())
          )
        }

        loadUploadedPhotos()
      }
    }
  }

  private fun loadUploadedPhotosInternal() {
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
          Success(loadPageOfUploadedPhotos(userId, lastUploadedOn, photosPerPage))
        } catch (error: Throwable) {
          Fail<List<UploadedPhoto>>(error)
        }

        val uploadedPhotos = request() ?: emptyList()
        val isEndReached = uploadedPhotos.isEmpty() || uploadedPhotos.size % photosPerPage != 0

        setState {
          copy(
            isEndReached = isEndReached,
            uploadedPhotosRequest = request,
            uploadedPhotos = state.uploadedPhotos + uploadedPhotos
          )
        }
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

  fun onUpdateReceiverInfo(receivedPhotos: List<ReceivedPhoto>) {
    if (receivedPhotos.isEmpty()) {
      return
    }

    withState { state ->
      val newPhotos = mutableListOf<UploadedPhoto>()

      for (receivedPhoto in receivedPhotos) {
        for (uploadedPhoto in state.uploadedPhotos) {
          newPhotos += if (uploadedPhoto.photoName == receivedPhoto.uploadedPhotoName) {
            uploadedPhoto.copy(hasReceiverInfo = true)
          } else {
            uploadedPhoto.copy()
          }
        }
      }

      setState { copy(uploadedPhotos = newPhotos) }
    }
  }

  fun onUploadingEvent(event: UploadedPhotosFragmentEvent.PhotoUploadEvent) {
    when (event) {
      is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingStart -> {
        Timber.tag(TAG).d("OnPhotoUploadingStart")

        withState { state ->
          val photoIndex = state.takenPhotos
            .indexOfFirst { it.id == event.photo.id && it.photoState == PhotoState.PHOTO_QUEUED_UP }

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
        }
      }
      is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress -> {
        Timber.tag(TAG).d("OnPhotoUploadingProgress")

        withState { state ->
          val photoIndex = state.takenPhotos
            .indexOfFirst { it.id == event.photo.id && it.photoState == PhotoState.PHOTO_UPLOADING }

          val newPhotos = if (photoIndex != -1) {
            state.takenPhotos.filter { it.id != event.photo.id }.toMutableList()
          } else {
            state.takenPhotos.toMutableList()
          }

          newPhotos.add(photoIndex, UploadingPhoto.fromMyPhoto(event.photo, event.progress))
          setState { copy(takenPhotos = newPhotos) }
        }
      }
      is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploaded -> {
        Timber.tag(TAG).d("OnPhotoUploaded")

        withState { state ->
          val photoIndex = state.takenPhotos
            .indexOfFirst { it.id == event.photo.id && it.photoState == PhotoState.PHOTO_UPLOADING }

          if (photoIndex == -1) {
            return@withState
          }

          val newPhoto = UploadedPhoto(
            event.newPhotoId,
            event.newPhotoName,
            event.currentLocation.lon,
            event.currentLocation.lat,
            false,
            event.uploadedOn,
            photoSize
          )

          val newTakenPhotos = state.takenPhotos
            .filter { it.id != event.photo.id }
            .toMutableList()

          val newUploadedPhotos = state.uploadedPhotos
            .toMutableList()

          newUploadedPhotos.add(0, newPhoto)

          setState {
            copy(
              takenPhotos = newTakenPhotos,
              uploadedPhotos = newUploadedPhotos
            )
          }
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
        }
      }
      is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnEnd -> {
        Timber.tag(TAG).d("OnEnd")
      }
    }.safe
  }

  /**
   * Called when parent's viewModel onClear method is called
   * */
  fun clear() {
    onCleared()
  }

  override fun onCleared() {
    super.onCleared()
    Timber.tag(TAG).d("onCleared()")

    compositeDisposable.dispose()
    job.cancel()
  }

  sealed class ActorAction {
    object ResetState : ActorAction()
    class CancelPhotoUploading(val photoId: Long) : ActorAction()
    object LoadQueuedUpPhotos : ActorAction()
    object LoadUploadedPhotos : ActorAction()
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