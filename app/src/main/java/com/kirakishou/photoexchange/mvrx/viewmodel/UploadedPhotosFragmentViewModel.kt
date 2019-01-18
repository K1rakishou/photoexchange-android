package com.kirakishou.photoexchange.mvrx.viewmodel

import androidx.fragment.app.FragmentActivity
import com.airbnb.mvrx.*
import com.kirakishou.fixmypc.photoexchange.BuildConfig
import com.kirakishou.photoexchange.helper.Constants
import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.helper.extension.filterDuplicatesWith
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.intercom.event.ReceivedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.usecases.GetFreshPhotosUseCase
import com.kirakishou.photoexchange.usecases.GetUploadedPhotosUseCase
import com.kirakishou.photoexchange.mvrx.model.NewReceivedPhoto
import com.kirakishou.photoexchange.mvrx.model.PhotoSize
import com.kirakishou.photoexchange.mvrx.model.photo.QueuedUpPhoto
import com.kirakishou.photoexchange.mvrx.model.photo.TakenPhoto
import com.kirakishou.photoexchange.mvrx.model.photo.UploadedPhoto
import com.kirakishou.photoexchange.mvrx.viewmodel.state.UpdateStateResult
import com.kirakishou.photoexchange.mvrx.viewmodel.state.UploadedPhotosFragmentState
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.fragment.ReceivedPhotosFragment
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import timber.log.Timber
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class UploadedPhotosFragmentViewModel(
  initialState: UploadedPhotosFragmentState,
  private val intercom: PhotosActivityViewModelIntercom,
  private val takenPhotosRepository: TakenPhotosRepository,
  private val uploadedPhotosRepository: UploadedPhotosRepository,
  private val getUploadedPhotosUseCase: GetUploadedPhotosUseCase,
  private val getFreshPhotosUseCase: GetFreshPhotosUseCase,
  private val dispatchersProvider: DispatchersProvider
) : BaseMvRxViewModel<UploadedPhotosFragmentState>(
  initialState,
  BuildConfig.DEBUG
), CoroutineScope {
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
          is ActorAction.OnNewPhotosReceived -> onNewPhotoReceivedInternal(action.newReceivedPhotos)
          is ActorAction.SwapPhotoAndMap -> swapPhotoAndMapInternal(action.photoName)
          ActorAction.CheckFreshPhotos -> checkFreshPhotosInternal()
        }.safe
      }
    }
  }

  //loads all photos taken by user that has not been uploaded to the server yet
  fun loadQueuedUpPhotos() {
    launch { viewModelActor.send(ActorAction.LoadQueuedUpPhotos) }
  }

  //resets everything to default state
  fun resetState(clearCache: Boolean) {
    launch { viewModelActor.send(ActorAction.ResetState(clearCache)) }
  }

  fun cancelPhotoUploading(photoId: Long) {
    launch { viewModelActor.send(ActorAction.CancelPhotoUploading(photoId)) }
  }

  //loads uploaded photos page by page
  fun loadUploadedPhotos(forced: Boolean) {
    launch { viewModelActor.send(ActorAction.LoadUploadedPhotos(forced)) }
  }

  //called when push notification is received
  fun onNewPhotosReceived(newReceivedPhotos: List<NewReceivedPhoto>) {
    launch { viewModelActor.send(ActorAction.OnNewPhotosReceived(newReceivedPhotos)) }
  }

  //switch between map and photo
  fun swapPhotoAndMap(photoName: String) {
    launch { viewModelActor.send(ActorAction.SwapPhotoAndMap(photoName)) }
  }

  //check whether there were uploaded new photos to the server (user may use the app from different divices)
  fun checkFreshPhotos() {
    launch { viewModelActor.send(ActorAction.CheckFreshPhotos) }
  }

  private fun checkFreshPhotosInternal() {
    withState { state ->
      //do not run the request if there are queued up photos
      if (state.takenPhotos.isNotEmpty()) {
        return@withState
      }

      //do not run the request if we are in the failed state
      if (state.uploadedPhotosRequest is Fail) {
        return@withState
      }

      //do not run the request if it is already running
      if (state.checkForFreshPhotosRequest is Loading) {
        return@withState
      }

      launch {
        val firstUploadedOn = state.uploadedPhotos
          .firstOrNull()
          ?.uploadedOn

        if (firstUploadedOn == null) {
          //no photos
          return@launch
        }

        val loadingState = Loading<Unit>()
        setState { copy(checkForFreshPhotosRequest = loadingState) }

        val freshPhotosRequest = try {
          Success(getFreshPhotosUseCase.getFreshUploadedPhotos(false, firstUploadedOn))
        } catch (error: Throwable) {
          Timber.tag(TAG).e(error, "Error while trying to check fresh uploaded photos")
          setState { copy(checkForFreshPhotosRequest = Uninitialized) }
          return@launch
        }

        val freshPhotos = freshPhotosRequest() ?: emptyList()
        if (freshPhotos.isEmpty()) {
          setState { copy(checkForFreshPhotosRequest = Uninitialized) }
          return@launch
        }

        val newPhotosCount = freshPhotos.count { it.uploadedOn > firstUploadedOn }

        //if there are any fresh photos - show snackbar
        if (newPhotosCount > 0) {
          intercom.tell<PhotosActivity>().to(PhotosActivityEvent.OnNewUploadedPhotos(newPhotosCount))
        }

        val newUploadedPhotos = state.uploadedPhotos
          .filterDuplicatesWith(freshPhotos) { it.photoName }
          .map { galleryPhoto -> galleryPhoto.copy(photoSize = photoSize) }
          .sortedByDescending { it.uploadedOn }

        setState {
          copy(
            uploadedPhotos = newUploadedPhotos,
            checkForFreshPhotosRequest = Uninitialized
          )
        }
      }
    }
  }

  private fun swapPhotoAndMapInternal(uploadedPhotoName: String) {
    withState { state ->
      val updateResult = state.swapPhotoAndMap(uploadedPhotoName)

      when (updateResult) {
        is UpdateStateResult.Update -> {
          setState { copy(uploadedPhotos = updateResult.update) }
        }
        is UpdateStateResult.SendIntercom -> {
          intercom.tell<PhotosActivity>().to(
            PhotosActivityEvent
              .ShowToast("Still looking for your photo...")
          )
        }
        is UpdateStateResult.NothingToUpdate -> {
        }
      }.safe
    }
  }

  // FIXME: make faster (use hashSet/Map for filtering already added photos)
  // This function is called every time a new page of received photos is loaded and
  // it is a pretty slow function
  private fun onNewPhotoReceivedInternal(newReceivedPhotos: List<NewReceivedPhoto>) {
    withState { state ->
      launch {
        val updatedPhotos = state.uploadedPhotos.toMutableList()

        for (newReceivedPhoto in newReceivedPhotos) {
          val photoIndex = updatedPhotos.indexOfFirst { uploadedPhoto ->
            uploadedPhoto.photoName == newReceivedPhoto.uploadedPhotoName
          }

          if (photoIndex == -1) {
            continue
          }

          if (updatedPhotos[photoIndex].receiverInfo != null) {
            //photo already has receiver info
            continue
          }

          val receiverInfo = UploadedPhoto.ReceiverInfo(
            newReceivedPhoto.receivedPhotoName,
            LonLat(
              newReceivedPhoto.lon,
              newReceivedPhoto.lat
            )
          )

          val updatedPhoto = updatedPhotos[photoIndex].copy(
            receiverInfo = receiverInfo
          )

          updatedPhotos.removeAt(photoIndex)
          updatedPhotos.add(photoIndex, updatedPhoto)
        }

        setState { copy(uploadedPhotos = updatedPhotos) }
      }
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

        val newTakenPhotos = state.takenPhotos.filterDuplicatesWith(notUploadedPhotos) { takenPhoto ->
          takenPhoto.id
        }

        setState { copy(takenPhotos = newTakenPhotos) }
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
        //to avoid "Your reducer must be pure!" exceptions
        val uploadedPhotosRequest = Loading<Paged<UploadedPhoto>>()
        setState { copy(uploadedPhotosRequest = uploadedPhotosRequest) }

        val firstUploadedOn = state.uploadedPhotos
          .firstOrNull()
          ?.uploadedOn

        val lastUploadedOn = state.uploadedPhotos
          .lastOrNull()
          ?.uploadedOn

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

        if (newPhotos.isNotEmpty()) {
          val mapped = newPhotos
            .filter { uploadedPhoto -> uploadedPhoto.receiverInfo != null }
            .map { uploadedPhoto ->
              uploadedPhoto.receiverInfo!!

              NewReceivedPhoto(
                uploadedPhoto.photoName,
                uploadedPhoto.receiverInfo.receiverPhotoName,
                uploadedPhoto.receiverInfo.receiverLonLat.lon,
                uploadedPhoto.receiverInfo.receiverLonLat.lat,
                uploadedPhoto.uploadedOn
              )
            }

          intercom.tell<ReceivedPhotosFragment>()
            .that(ReceivedPhotosFragmentEvent.GeneralEvents.OnNewPhotosReceived(mapped))
        }

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
          val updateResult = state.onPhotoUploadingStart(event.photo)
          if (updateResult !is UpdateStateResult.Update) {
            throw IllegalStateException("Not implemented for result ${updateResult::class}")
          }

          setState { copy(takenPhotos = updateResult.update) }
        }
      }
      is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress -> {
        Timber.tag(TAG).d("OnPhotoUploadingProgress")

        withState { state ->
          val updateResult = state.onPhotoUploadingProgress(event.photo, event.progress)
          if (updateResult !is UpdateStateResult.Update) {
            throw IllegalStateException("Not implemented for result ${updateResult::class}")
          }

          setState { copy(takenPhotos = updateResult.update) }
        }
      }
      is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploaded -> {
        Timber.tag(TAG).d("OnPhotoUploaded")

        withState { state ->
          val updateResult = state.onPhotoUploaded(
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
          val updateResult = state.onFailedToUploadPhoto(event.photo)
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
      is UploadedPhotosFragmentEvent.ReceivePhotosEvent.OnPhotosReceived -> {
        Timber.tag(TAG).d("OnPhotosReceived")

        if (event.receivedPhotos.isEmpty()) {
          return
        }

        withState { state ->
          val updateResult = state.onPhotosReceived(event.receivedPhotos)
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
    intercom.tell<PhotosActivity>().to(PhotosActivityEvent.StartReceivingService)
  }

  private fun startUploadingService() {
    Timber.tag(TAG).d("startUploadingService called!")
    intercom.tell<PhotosActivity>().to(PhotosActivityEvent.StartUploadingService)
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

  /**
   * For tests
   * */

  fun testSetState(newState: UploadedPhotosFragmentState) {
    setState { newState }
  }

  suspend fun testGetState(): UploadedPhotosFragmentState {
    return suspendCoroutine { continuation ->
      withState { state -> continuation.resume(state) }
    }
  }

  sealed class ActorAction {
    class ResetState(val clearCache: Boolean) : ActorAction()
    class CancelPhotoUploading(val photoId: Long) : ActorAction()
    object LoadQueuedUpPhotos : ActorAction()
    class LoadUploadedPhotos(val forced: Boolean) : ActorAction()
    class OnNewPhotosReceived(val newReceivedPhotos: List<NewReceivedPhoto>) : ActorAction()
    class SwapPhotoAndMap(val photoName: String) : ActorAction()
    object CheckFreshPhotos : ActorAction()
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