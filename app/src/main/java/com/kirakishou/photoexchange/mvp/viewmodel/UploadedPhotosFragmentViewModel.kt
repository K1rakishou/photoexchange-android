package com.kirakishou.photoexchange.mvp.viewmodel

import androidx.fragment.app.FragmentActivity
import com.airbnb.mvrx.*
import com.kirakishou.fixmypc.photoexchange.BuildConfig
import com.kirakishou.photoexchange.helper.Constants
import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.helper.extension.filterDuplicatesWith
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.interactors.GetUploadedPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.PhotoExchangedData
import com.kirakishou.photoexchange.mvp.model.PhotoSize
import com.kirakishou.photoexchange.mvp.model.photo.QueuedUpPhoto
import com.kirakishou.photoexchange.mvp.model.photo.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.photo.UploadedPhoto
import com.kirakishou.photoexchange.mvp.viewmodel.state.UpdateStateResult
import com.kirakishou.photoexchange.mvp.viewmodel.state.UploadedPhotosFragmentState
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

class UploadedPhotosFragmentViewModel(
  initialState: UploadedPhotosFragmentState,
  private val intercom: PhotosActivityViewModelIntercom,
  private val takenPhotosRepository: TakenPhotosRepository,
  private val uploadedPhotosRepository: UploadedPhotosRepository,
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
          is ActorAction.LoadUploadedPhotos -> loadUploadedPhotosInternal(action.forced)
          is ActorAction.OnNewPhotoReceived -> onNewPhotoReceivedInternal(action.photoExchangedData)
          is ActorAction.SwapPhotoAndMap -> swapPhotoAndMapInternal(action.photoName)
        }.safe
      }
    }
  }

  fun loadQueuedUpPhotos() {
    launch { viewModelActor.send(ActorAction.LoadQueuedUpPhotos) }
  }

  fun resetState(clearCache: Boolean) {
    launch { viewModelActor.send(ActorAction.ResetState(clearCache)) }
  }

  fun cancelPhotoUploading(photoId: Long) {
    launch { viewModelActor.send(ActorAction.CancelPhotoUploading(photoId)) }
  }

  fun loadUploadedPhotos(forced: Boolean) {
    launch { viewModelActor.send(ActorAction.LoadUploadedPhotos(forced)) }
  }

  fun onNewPhotoReceived(photoExchangedData: PhotoExchangedData) {
    launch { viewModelActor.send(ActorAction.OnNewPhotoReceived(photoExchangedData)) }
  }

  fun swapPhotoAndMap(photoName: String) {
    launch { viewModelActor.send(ActorAction.SwapPhotoAndMap(photoName)) }
  }

  private fun swapPhotoAndMapInternal(uploadedPhotoName: String) {
    withState { state ->
      val updateResult = state.swapPhotoAndMap(uploadedPhotoName)

      when (updateResult) {
        is UpdateStateResult.Update -> {
          setState { copy(uploadedPhotos = updateResult.update) }
        }
        is UpdateStateResult.SendIntercom -> {
          intercom.tell<PhotosActivity>().to(PhotosActivityEvent
            .ShowToast("Still looking for your photo..."))
        }
        is UpdateStateResult.NothingToUpdate -> {}
      }.safe

    }
  }

  private fun onNewPhotoReceivedInternal(photoExchangedData: PhotoExchangedData) {
    withState { state ->
      val updateResult = state.updateReceiverInfo(photoExchangedData)

      when (updateResult) {
        is UpdateStateResult.Update -> {
          setState { copy(uploadedPhotos = updateResult.update) }
        }
        is UpdateStateResult.SendIntercom -> {}
        is UpdateStateResult.NothingToUpdate -> {}
      }.safe
    }
  }

  private fun resetStateInternal(clearCache: Boolean) {
    launch {
      if (clearCache) {
        uploadedPhotosRepository.deleteAll()
      }

      //to avoid "Your reducer must be pure!" exceptions
      val newState = UploadedPhotosFragmentState()
      setState { newState }

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

        val message = "Error has occurred while trying to cancel photo uploading. " +
          "\nError message: ${error.message ?: "Unknown error message"}"
        intercom.tell<PhotosActivity>()
          .to(PhotosActivityEvent.ShowToast(message))
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
          loadUploadedPhotos(false)
          return@launch
        }

        setState {
          copy(
            takenPhotos = state.takenPhotos + notUploadedPhotos
          )
        }

        startUploadingService()
      }
    }
  }

  private fun loadUploadedPhotosInternal(forced: Boolean) {
    withState { state ->
      if (state.uploadedPhotosRequest is Loading) {
        return@withState
      }

      launch {
        val firstUploadedOn = state.uploadedPhotos
          .firstOrNull()
          ?.uploadedOn
          ?: -1L

        val lastUploadedOn = state.uploadedPhotos
          .lastOrNull()
          ?.uploadedOn
          ?: -1L

        //to avoid "Your reducer must be pure!" exceptions
        val uploadedPhotosRequest = Loading<Paged<UploadedPhoto>>()
        setState { copy(uploadedPhotosRequest = uploadedPhotosRequest) }

        val request = try {
          val photos = getUploadedPhotosUseCase.loadPageOfPhotos(
            forced,
            firstUploadedOn,
            lastUploadedOn,
            photosPerPage
          )

          Success(photos)
        } catch (error: Throwable) {
          Timber.tag(TAG).e(error)
          Fail<Paged<UploadedPhoto>>(error)
        }

        val newPhotos = (request()?.page ?: emptyList())
        val newUploadedPhotos = state.uploadedPhotos
          .filterDuplicatesWith(newPhotos) { it.photoName }
          .map { uploadedPhoto -> uploadedPhoto.copy(photoSize = photoSize) }
          .sortedByDescending { it.uploadedOn }

        val isEndReached = request()?.isEnd ?: false

        setState {
          copy(
            isEndReached = isEndReached,
            uploadedPhotosRequest = request,
            uploadedPhotos = newUploadedPhotos
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
          val updateResult = state.replaceQueuedUpPhotoWithUploading(event.photo)
          if (updateResult !is UpdateStateResult.Update) {
            throw IllegalStateException("Not implemented for result ${updateResult::class}")
          }

          setState { copy(takenPhotos = updateResult.update) }
        }
      }
      is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress -> {
        Timber.tag(TAG).d("OnPhotoUploadingProgress")

        withState { state ->
          val updateResult = state.updateUploadingPhotoProgress(event.photo, event.progress)
          if (updateResult !is UpdateStateResult.Update) {
            throw IllegalStateException("Not implemented for result ${updateResult::class}")
          }

          setState { copy(takenPhotos = updateResult.update) }
        }
      }
      is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploaded -> {
        Timber.tag(TAG).d("OnPhotoUploaded")

        withState { state ->
          val updateResult = state.replaceUploadingPhotoWithUploaded(
            event.photo,
            event.newPhotoId,
            event.newPhotoName,
            event.uploadedOn,
            event.currentLocation
          )

          if (updateResult !is UpdateStateResult.Update) {
            throw IllegalStateException("Not implemented for result ${updateResult::class}")
          }

          val (newTakenPhotos, newUploadedPhotos) = updateResult.update
          setState { copy(takenPhotos = newTakenPhotos, uploadedPhotos = newUploadedPhotos) }
        }
      }
      is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnFailedToUploadPhoto -> {
        Timber.tag(TAG).d("OnFailedToUploadPhoto")

        withState { state ->
          val updateResult = state.replaceUploadingPhotoWithFailed(event.photo)
          if (updateResult !is UpdateStateResult.Update) {
            throw IllegalStateException("Not implemented for result ${updateResult::class}")
          }

          setState { copy(takenPhotos = updateResult.update) }

          intercom.tell<PhotosActivity>()
            .to(PhotosActivityEvent.ShowToast("Failed to upload photo, error message: ${event.error.message}"))
        }
      }
      is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoCanceled -> {
        Timber.tag(TAG).d("OnPhotoCanceled")
        cancelPhotoUploading(event.photo.id)
      }
      is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnError -> {
        Timber.tag(TAG).d("OnError")

        withState { state ->
          val newPhotos = state.takenPhotos
            .map { takenPhoto -> QueuedUpPhoto.fromTakenPhoto(takenPhoto) }

          setState { copy(takenPhotos = newPhotos) }
        }
      }
      is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnEnd -> {
        Timber.tag(TAG).d("OnEnd")
        startReceivingService()
      }
    }.safe
  }

  fun onReceiveEvent(event: UploadedPhotosFragmentEvent.ReceivePhotosEvent) {
    when (event) {
      is UploadedPhotosFragmentEvent.ReceivePhotosEvent.PhotosReceived -> {
        Timber.tag(TAG).d("PhotosReceived")

        if (event.receivedPhotos.isEmpty()) {
          return
        }

        withState { state ->
          val updateResult = state.updateReceiverInfo(event.receivedPhotos)
          if (updateResult !is UpdateStateResult.Update) {
            throw IllegalStateException("Not implemented for result ${updateResult::class}")
          }

          setState { copy(uploadedPhotos = updateResult.update) }
        }
      }
      is UploadedPhotosFragmentEvent.ReceivePhotosEvent.OnFailed -> {
        Timber.tag(TAG).d("OnFailed")

        event.error.printStackTrace()
        Timber.tag(TAG).d("Error while trying to receive photos: (${event.error.message})")
      }
    }.safe
  }

  private fun startReceivingService() {
    Timber.tag(TAG).d("startReceiveingService called!")

    intercom.tell<PhotosActivity>()
      .to(PhotosActivityEvent.StartReceivingService(
        PhotosActivityViewModel::class.java,
        "Start receiving service request")
      )
  }

  private fun startUploadingService() {
    Timber.tag(TAG).d("startUploadingService called!")

    intercom.tell<PhotosActivity>()
      .to(PhotosActivityEvent.StartUploadingService(
        PhotosActivityViewModel::class.java,
        "Start uploading service request")
      )
  }

  /**
   * Called when parent's viewModel onClear method is called
   * */
  fun clear() {
    onCleared()
  }

  override fun onCleared() {
    super.onCleared()

    compositeDisposable.clear()
    job.cancelChildren()
  }

  sealed class ActorAction {
    class ResetState(val clearCache: Boolean) : ActorAction()
    class CancelPhotoUploading(val photoId: Long) : ActorAction()
    object LoadQueuedUpPhotos : ActorAction()
    class LoadUploadedPhotos(val forced: Boolean) : ActorAction()
    class OnNewPhotoReceived(val photoExchangedData: PhotoExchangedData) : ActorAction()
    class SwapPhotoAndMap(val photoName: String) : ActorAction()
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