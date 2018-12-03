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
import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.PhotoExchangedData
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
  private val timeUtils: TimeUtils,
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
          is ActorAction.OnNewPhotoReceived -> onNewPhotoReceivedInternal(action.photoExchangedData)
          is ActorAction.SwapPhotoAndMap -> swapPhotoAndMapInternal(action.photoName)
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

  fun onNewPhotoReceived(photoExchangedData: PhotoExchangedData) {
    launch { viewModelActor.send(ActorAction.OnNewPhotoReceived(photoExchangedData)) }
  }

  fun swapPhotoAndMap(photoName: String) {
    launch { viewModelActor.send(ActorAction.SwapPhotoAndMap(photoName)) }
  }

  private fun swapPhotoAndMapInternal(uploadedPhotoName: String) {
    withState { state ->
      val photoIndex = state.uploadedPhotos.indexOfFirst { it.photoName == uploadedPhotoName }
      if (photoIndex == -1) {
        return@withState
      }

      if (state.uploadedPhotos[photoIndex].receiverInfo == null) {
        intercom.tell<PhotosActivity>().to(PhotosActivityEvent
          .ShowToast("Still looking for your photo..."))
        return@withState
      }

      val oldShowPhoto = state.uploadedPhotos[photoIndex].showPhoto
      val updatedPhoto = state.uploadedPhotos[photoIndex]
        .copy(showPhoto = !oldShowPhoto)

      val updatedPhotos = state.uploadedPhotos.toMutableList()
      updatedPhotos[photoIndex] = updatedPhoto

      setState { copy(uploadedPhotos = updatedPhotos) }
    }
  }

  private fun onNewPhotoReceivedInternal(photoExchangedData: PhotoExchangedData) {
    withState { state ->
      val photoIndex = state.uploadedPhotos
        .indexOfFirst { it.photoName == photoExchangedData.uploadedPhotoName }
      if (photoIndex == -1) {
        //nothing to update
        return@withState
      }

      val updatedPhotos = state.uploadedPhotos.toMutableList()
      val receiverInfo = UploadedPhoto.ReceiverInfo(
        photoExchangedData.receivedPhotoName,
        photoExchangedData.lon,
        photoExchangedData.lat
      )

      val updatedPhoto = updatedPhotos[photoIndex]
        .copy(receiverInfo = receiverInfo)

      updatedPhotos.removeAt(photoIndex)
      updatedPhotos.add(photoIndex, updatedPhoto)

      setState { copy(uploadedPhotos = updatedPhotos) }
    }
  }


  private fun fetchFreshPhotosInternal() {
    /**
     * Combines old uploadedPhotos with the fresh ones and also counts how many of the fresh photos were
     * truly fresh (e.g. uploadedPhotos didn't contain them yet or it did but without receiverInfo)
     * */
    suspend fun combinePhotos(
      freshPhotos: List<UploadedPhoto>,
      uploadedPhotos: List<UploadedPhoto>
    ): Pair<MutableList<UploadedPhoto>, Int> {
      val updatedPhotos = uploadedPhotos.toMutableList()
      //how much photos were updated
      var freshPhotosCount = 0

      for (freshPhoto in freshPhotos) {
        if (!uploadedPhotosRepository.contains(freshPhoto.photoName)) {
          //if we don't have this photo yet - add it to the list
          updatedPhotos += freshPhoto
          ++freshPhotosCount
          continue
        }

        val uploadedPhotoIndex = updatedPhotos.indexOfFirst { it.photoName == freshPhoto.photoName }
        if (uploadedPhotoIndex != -1) {
          val uploadedPhoto = uploadedPhotos[uploadedPhotoIndex]

          //if we already have this photo but old photo has no receiverInfo and the new one has
          if (uploadedPhoto.receiverInfo == null && freshPhoto.receiverInfo != null) {
            //replace it with the newer one
            updatedPhotos.removeAt(uploadedPhotoIndex)
            updatedPhotos += freshPhoto

            ++freshPhotosCount
          }
        }
      }

      return updatedPhotos to freshPhotosCount
    }

    //TODO: should we run this method if uploadedPhotosRequest is in the Failed or Loading state?
    withState { state ->
      launch {
        //if we are trying to fetch fresh photos and there are no uploadedPhotos - start normal photos loading
        if (state.uploadedPhotos.isEmpty()) {
          loadQueuedUpPhotos()
          return@launch
        }

        val freshPhotos = try {
          getUploadedPhotosUseCase.loadFreshPhotos(timeUtils.getTimeFast(), photosPerPage)
        } catch (error: Throwable) {
          Timber.tag(TAG).e(error)

          val message = "Error has occurred while trying to fetch fresh photos. \nError message: ${error.message
            ?: "Unknown error message"}"
          intercom.tell<PhotosActivity>()
            .to(PhotosActivityEvent.ShowToast(message))
          return@launch
        }

        val (combinedPhotos, freshPhotosCount) = combinePhotos(freshPhotos.page, state.uploadedPhotos)
        if (freshPhotosCount == 0) {
          //Should this even happen? We are supposed to have new photos if this method was called.
          //Update: Yes this can happen! When user has more than "photosPerPage" uploaded photos without receiverInfo

          Timber.tag(TAG).d("combinePhotos returned 0 freshPhotosCount!")
          return@launch
        }

        if (freshPhotosCount >= photosPerPage) {
          //this means that there are probably even more photos that this user has not seen yet
          //so we have no other option but to clear database cache and reload everything

          Timber.tag(TAG).d("combinePhotos returned more or the same amount of freshPhotos that we have requested")
          resetState(true)
          return@launch
        }

        val sortedPhotos = combinedPhotos
          .map { uploadedPhoto -> uploadedPhoto.copy(photoSize = photoSize) }
          .sortedByDescending { it.uploadedOn }
        setState { copy(uploadedPhotos = sortedPhotos) }
      }
    }
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

        val message = "Error has occurred while trying to cancel photo uploading. \nError message: ${error.message
          ?: "Unknown error message"}"
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
          Success(getUploadedPhotosUseCase.loadPageOfPhotos(lastUploadedOn, photosPerPage))
        } catch (error: Throwable) {
          Fail<Paged<UploadedPhoto>>(error)
        }

        val oldPhotoNameSet = state.uploadedPhotos
          .map { it.photoName }
          .toSet()

        val newUploadedPhotos = (request()?.page ?: emptyList())
          .filterNot { oldPhotoNameSet.contains(it.photoName) }
          .map { uploadedPhoto -> uploadedPhoto.copy(photoSize = photoSize) }

        val sortedPhotos = (newUploadedPhotos + state.uploadedPhotos)
          .sortedByDescending { it.uploadedOn }

        val isEndReached = request()?.isEnd ?: false

        setState {
          copy(
            isEndReached = isEndReached,
            uploadedPhotosRequest = request,
            uploadedPhotos = sortedPhotos
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
        Timber.tag(TAG).d("OnFailedToUploadPhoto")

        withState { state ->
          val photoIndex = state.takenPhotos.indexOfFirst { it.id == event.photo.id }
          val newPhotos = if (photoIndex != -1) {
            state.takenPhotos.filter { it.id != event.photo.id }.toMutableList()
          } else {
            state.takenPhotos.toMutableList()
          }

          newPhotos.add(photoIndex, QueuedUpPhoto.fromTakenPhoto(event.photo))
          setState { copy(takenPhotos = newPhotos) }

          intercom.tell<PhotosActivity>()
            .to(PhotosActivityEvent.ShowToast("Failed to upload photo"))
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
                exchangedPhoto.receivedPhotoName,
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
        //do nothing?
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