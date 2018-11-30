package com.kirakishou.photoexchange.mvp.viewmodel

import androidx.fragment.app.FragmentActivity
import com.airbnb.mvrx.*
import com.kirakishou.fixmypc.photoexchange.BuildConfig
import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.mvp.model.PhotoSize
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.interactors.GetUploadedPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.photo.UploadedPhoto
import com.kirakishou.photoexchange.helper.Constants
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.mvp.model.photo.QueuedUpPhoto
import com.kirakishou.photoexchange.mvp.model.photo.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.photo.UploadingPhoto
import com.kirakishou.photoexchange.mvp.viewmodel.state.UploadedPhotosFragmentState
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
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
  private val uploadedPhotosRepository: UploadedPhotosRepository,
  private val receivedPhotosRepository: ReceivedPhotosRepository,
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
          is ActorAction.ResetState -> resetStateInternal(action.clearCache)
          is ActorAction.CancelPhotoUploading -> cancelPhotoUploadingInternal(action.photoId)
          ActorAction.LoadQueuedUpPhotos -> loadQueuedUpPhotosInternal()
          ActorAction.LoadUploadedPhotos -> loadUploadedPhotosInternal()
          ActorAction.FetchFreshPhotos -> fetchFreshPhotosInternal()
        }.safe
      }
    }

    loadQueuedUpPhotos()
  }

  fun resetState(clearCache: Boolean = false) {
    launch { viewModelActor.send(ActorAction.ResetState(clearCache)) }
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

  fun fetchFreshPhotos() {
    launch { viewModelActor.send(ActorAction.FetchFreshPhotos) }
  }

  private fun fetchFreshPhotosInternal() {
    launch {
      //if we are trying to fetch fresh photos and the database is empty - start normal photos loading
      val uploadedPhotosCount = uploadedPhotosRepository.count()
      if (uploadedPhotosCount == 0) {
        loadQueuedUpPhotos()
        return@launch
      }


    }
    //TODO
  }

  private fun resetStateInternal(clearCache: Boolean) {
    launch {
      if (clearCache) {
        uploadedPhotosRepository.deleteAll()
      }

      setState { UploadedPhotosFragmentState() }
      loadQueuedUpPhotos()
    }
  }

  private fun cancelPhotoUploadingInternal(photoId: Long) {
    launch {
      try {
        if (takenPhotosRepository.findById(photoId) == null) {
          return@launch
        }

        if (!takenPhotosRepository.deletePhotoById(photoId)) {
          throw DatabaseException("Could not delete photo with id ${photoId}")
        }
      } catch (error: Throwable) {
        Timber.tag(TAG).e(error)

        //TODO: show a toast that we could not cancel the photo
        return@launch
      }

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

        val notUploadedPhotos = (request() ?: emptyList())
        if (notUploadedPhotos.isEmpty() && state.takenPhotos.isEmpty()) {
          loadUploadedPhotos()
          return@launch
        }

        setState {
          copy(
            takenPhotos = state.takenPhotos + notUploadedPhotos
          )
        }

        startUploadingService("There are queued up photos that need to be uploaded")
      }
    }
  }

  private fun loadUploadedPhotosInternal() {
    withState { state ->
      if (state.uploadedPhotosRequest is Loading) {
        return@withState
      }

      launch {
        val lastUploadedOn = state.uploadedPhotos
          .lastOrNull()
          ?.uploadedOn
          ?: -1L

        setState { copy(uploadedPhotosRequest = Loading()) }

        val request = try {
          uploadedPhotosRepository.deleteOldPhotos()

          val result = getUploadedPhotosUseCase.loadPageOfPhotos(lastUploadedOn, photosPerPage)
          if (result is Either.Error) {
            throw result.error
          }

          result as Either.Value

          val uploadedPhotos = result.value
            .map { uploadedPhoto -> uploadedPhoto.copy(photoSize = photoSize) }

          Success(uploadedPhotos)
        } catch (error: Throwable) {
          Fail<List<UploadedPhoto>>(error)
        }

        val newUploadedPhotos = request() ?: emptyList()
        val isEndReached = newUploadedPhotos.size < photosPerPage

        val hasPhotosWithNoReceiver = newUploadedPhotos.any { it.receiverInfo == null }
        if (hasPhotosWithNoReceiver) {
          startReceivingService("There are photos with no receiver info")
        }

        setState {
          copy(
            isEndReached = isEndReached,
            uploadedPhotosRequest = request,
            uploadedPhotos = state.uploadedPhotos + newUploadedPhotos
          )
        }
      }
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

          if (photoIndex != -1) {
            newPhotos.add(photoIndex, UploadingPhoto.fromMyPhoto(event.photo, event.progress))
          } else {
            newPhotos.add(0, UploadingPhoto.fromMyPhoto(event.photo, event.progress))
          }

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

          val newTakenPhotos = state.takenPhotos.toMutableList()
          newTakenPhotos.removeAt(photoIndex)

          val newUploadedPhotos = state.uploadedPhotos.toMutableList()
          val newUploadedPhoto = UploadedPhoto(
            event.newPhotoId,
            event.newPhotoName,
            event.currentLocation.lon,
            event.currentLocation.lat,
            null,
            event.uploadedOn
          )

          newUploadedPhotos.add(newUploadedPhoto)
          newUploadedPhotos.sortByDescending { it.uploadedOn }

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

        launch {
          // if we can't start a service to receive photos (not enough photos uploaded) -
          // show already uploaded photos
          if (!startReceivingService("Photos uploading done")) {
            loadUploadedPhotos()
          }
        }

        Unit
      }
    }.safe
  }

  fun onReceiveEvent(event: UploadedPhotosFragmentEvent.ReceivePhotosEvent) {
    Timber.tag(TAG).d("onReceiveEvent")

    when (event) {
      is UploadedPhotosFragmentEvent.ReceivePhotosEvent.PhotosReceived -> {
        if (event.receivedPhotos.isEmpty()) {
          return
        }

        withState { state ->
          val updatedPhotos = mutableListOf<UploadedPhoto>()

          for (uploadedPhoto in state.uploadedPhotos) {
            val exchangedPhoto = event.receivedPhotos
              .firstOrNull { it.uploadedPhotoName == uploadedPhoto.photoName }

            updatedPhotos += if (exchangedPhoto == null) {
              uploadedPhoto.copy()
            } else {
              val receiverInfo = UploadedPhoto.ReceiverInfo(
                exchangedPhoto.lon,
                exchangedPhoto.lat
              )

              uploadedPhoto.copy(receiverInfo = receiverInfo)
            }
          }

          setState { copy(uploadedPhotos = updatedPhotos) }
        }
      }
      is UploadedPhotosFragmentEvent.ReceivePhotosEvent.NoPhotosReceived -> {

      }
      is UploadedPhotosFragmentEvent.ReceivePhotosEvent.OnFailed -> {
        event.error.printStackTrace()
        Timber.tag(TAG).d("Error while trying to receive photos: (${event.error.message})")
      }
    }.safe

    loadUploadedPhotos()
  }

  private fun startUploadingService(reason: String) {
    intercom.tell<PhotosActivity>()
      .to(PhotosActivityEvent.StartUploadingService(
        PhotosActivityViewModel::class.java,
        reason)
      )
  }

  private suspend fun startReceivingService(reason: String): Boolean {
    val uploadedPhotosCount = uploadedPhotosRepository.count()
    val receivedPhotosCount = receivedPhotosRepository.count()

    val canReceivePhotos = uploadedPhotosCount > receivedPhotosCount
    if (!canReceivePhotos) {
      return false
    }

    intercom.tell<PhotosActivity>()
      .to(PhotosActivityEvent.StartReceivingService(
        PhotosActivityViewModel::class.java,
        reason)
      )

    return true
  }

  /**
   * Called when parent's viewModel onClear method is called
   * */
  fun clear() {
    onCleared()
  }

  override fun onCleared() {
    super.onCleared()

    compositeDisposable.dispose()
    job.cancel()
  }

  sealed class ActorAction {
    class ResetState(val clearCache: Boolean) : ActorAction()
    class CancelPhotoUploading(val photoId: Long) : ActorAction()
    object LoadQueuedUpPhotos : ActorAction()
    object LoadUploadedPhotos : ActorAction()
    object FetchFreshPhotos : ActorAction()
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