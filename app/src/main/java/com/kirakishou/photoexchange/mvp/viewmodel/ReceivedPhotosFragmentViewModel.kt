package com.kirakishou.photoexchange.mvp.viewmodel

import androidx.fragment.app.FragmentActivity
import com.airbnb.mvrx.*
import com.kirakishou.fixmypc.photoexchange.BuildConfig
import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.mvp.model.PhotoSize
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.helper.intercom.event.ReceivedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.interactors.GetReceivedPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.photo.ReceivedPhoto
import com.kirakishou.photoexchange.helper.Constants
import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.PhotoExchangedData
import com.kirakishou.photoexchange.mvp.viewmodel.state.ReceivedPhotosFragmentState
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

class ReceivedPhotosFragmentViewModel(
  initialState: ReceivedPhotosFragmentState,
  private val intercom: PhotosActivityViewModelIntercom,
  private val timeUtils: TimeUtils,
  private val receivedPhotosRepository: ReceivedPhotosRepository,
  private val getReceivedPhotosUseCase: GetReceivedPhotosUseCase,
  private val dispatchersProvider: DispatchersProvider
) : BaseMvRxViewModel<ReceivedPhotosFragmentState>(initialState, BuildConfig.DEBUG), CoroutineScope {
  private val TAG = "ReceivedPhotosFragmentViewModel"

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
          ActorAction.LoadReceivedPhotos -> loadReceivedPhotosInternal()
          is ActorAction.ResetState -> resetStateInternal(action.clearCache)
          is ActorAction.SwapPhotoAndMap -> swapPhotoAndMapInternal(action.receivedPhotoName)
          ActorAction.FetchFreshPhotos -> fetchFreshPhotosInternal()
          is ActorAction.OnNewPhotoReceived -> onNewPhotoReceivedInternal(action.photoExchangedData)
        }.safe
      }
    }

    loadReceivedPhotos()
  }

  fun loadReceivedPhotos() {
    launch { viewModelActor.send(ActorAction.LoadReceivedPhotos) }
  }

  fun resetState(clearCache: Boolean = false) {
    launch { viewModelActor.send(ActorAction.ResetState(clearCache)) }
  }

  fun swapPhotoAndMap(receivedPhotoName: String) {
    launch { viewModelActor.send(ActorAction.SwapPhotoAndMap(receivedPhotoName)) }
  }

  fun fetchFreshPhotos() {
    launch { viewModelActor.send(ActorAction.FetchFreshPhotos) }
  }

  fun onNewPhotoReceived(photoExchangedData: PhotoExchangedData) {
    launch { viewModelActor.send(ActorAction.OnNewPhotoReceived(photoExchangedData)) }
  }

  private fun onNewPhotoReceivedInternal(photoExchangedData: PhotoExchangedData) {
    withState { state ->
      val photoIndex = state.receivedPhotos.indexOfFirst { it.receivedPhotoName == photoExchangedData.receivedPhotoName }
      if (photoIndex != -1) {
        //photo is already shown
        return@withState
      }

      val newPhoto = ReceivedPhoto(
        photoExchangedData.uploadedPhotoName,
        photoExchangedData.receivedPhotoName,
        photoExchangedData.lon,
        photoExchangedData.lat,
        photoExchangedData.uploadedOn,
        true,
        photoSize
      )

      val updatedPhotos = state.receivedPhotos.toMutableList() + newPhoto
      val sortedPhotos = updatedPhotos
        .sortedByDescending { it.uploadedOn }

      //show a snackbar telling user that we got a photo
      intercom.tell<PhotosActivity>()
        .that(PhotosActivityEvent.OnNewPhotoReceived)

      setState { copy(receivedPhotos = sortedPhotos) }
    }
  }

  private fun fetchFreshPhotosInternal() {
    /**
     * Combines old receivedPhotos with the fresh ones and also counts how many of the fresh photos were
     * truly fresh (e.g. receivedPhotos didn't contain them yet)
     * */
    suspend fun combinePhotos(
      freshPhotos: List<ReceivedPhoto>,
      receivedPhotos: List<ReceivedPhoto>
    ): Pair<MutableList<ReceivedPhoto>, Int> {
      val updatedPhotos = receivedPhotos.toMutableList()
      var freshPhotosCount = 0

      for (freshPhoto in freshPhotos) {
        if (!receivedPhotosRepository.contains(freshPhoto.uploadedPhotoName)) {
          //if we don't have this photo yet - add it to the list
          updatedPhotos += freshPhoto
          ++freshPhotosCount
        }
      }

      return updatedPhotos to freshPhotosCount
    }

    //TODO: should we run this method if receivedPhotosRequest is in the Failed or Loading state?
    withState { state ->
      launch {
        //if we are trying to fetch fresh photos and there are no uploadedPhotos - start normal photos loading
        if (state.receivedPhotos.isEmpty()) {
          loadReceivedPhotos()
          return@launch
        }

        val freshPhotos = try {
          getReceivedPhotosUseCase.loadFreshPhotos(timeUtils.getTimeFast(), photosPerPage)
        } catch (error: Throwable) {
          Timber.tag(TAG).e(error)

          val message = "Error has occurred while trying to fetch fresh photos. \nError message: ${error.message ?: "Unknown error message"}"
          intercom.tell<PhotosActivity>()
            .to(PhotosActivityEvent.ShowToast(message))
          return@launch
        }

        val (combinedPhotos, freshPhotosCount) = combinePhotos(freshPhotos.page, state.receivedPhotos)
        if (freshPhotosCount == 0) {
          //Should this even happen? We are supposed to have new photos if this method was called.
          //Update: Yes this can happen! When user has more than "photosPerPage" fresh received photos

          Timber.tag(TAG).d("combinePhotos returned 0 freshPhotosCount!")
          resetState(true)
          return@launch
        }

        if (freshPhotosCount >= photosPerPage) {
          //this means that there are probably even more photos that this user has not seen yet
          //so we have no other option but to clear database cache and reload everything

          Timber.tag(TAG).d("combinePhotos method more or the same amount of freshPhotos that we have requested")
          resetState(true)
          return@launch
        }

        val sortedPhotos = combinedPhotos
          .map { uploadedPhoto -> uploadedPhoto.copy(photoSize = photoSize) }
          .sortedByDescending { it.uploadedOn }
        setState { copy(receivedPhotos = sortedPhotos) }
      }
    }
  }

  private fun swapPhotoAndMapInternal(receivedPhotoName: String) {
    withState { state ->
      val photoIndex = state.receivedPhotos.indexOfFirst { it.receivedPhotoName == receivedPhotoName }
      if (photoIndex == -1) {
        return@withState
      }

      val oldShowPhoto = state.receivedPhotos[photoIndex].showPhoto
      val updatedPhoto = state.receivedPhotos[photoIndex]
        .copy(showPhoto = !oldShowPhoto)

      val updatedPhotos = state.receivedPhotos.toMutableList()
      updatedPhotos[photoIndex] = updatedPhoto

      setState { copy(receivedPhotos = updatedPhotos) }
    }
  }

  private fun resetStateInternal(clearCache: Boolean) {
    launch {
      if (clearCache) {
        receivedPhotosRepository.deleteAll()
      }

      setState { ReceivedPhotosFragmentState() }
      viewModelActor.send(ActorAction.LoadReceivedPhotos)
    }
  }

  private suspend fun loadReceivedPhotosInternal() {
    withState { state ->
      if (state.receivedPhotosRequest is Loading) {
        return@withState
      }

      launch {
        val lastUploadedOn = state.receivedPhotos
          .lastOrNull()
          ?.uploadedOn
          ?: -1L

        setState { copy(receivedPhotosRequest = Loading()) }

        val request = try {
          receivedPhotosRepository.deleteOldPhotos()
          val receivedPhotos = getReceivedPhotosUseCase.loadPageOfPhotos(lastUploadedOn, photosPerPage)

          intercom.tell<UploadedPhotosFragment>()
            .to(UploadedPhotosFragmentEvent.ReceivePhotosEvent.PhotosReceived(receivedPhotos.page.map { it }))

          Success(receivedPhotos)
        } catch (error: Throwable) {
          Fail<Paged<ReceivedPhoto>>(error)
        }

        val newReceivedPhotos = ((request()?.page ?: emptyList()) + state.receivedPhotos)
          .map { uploadedPhoto -> uploadedPhoto.copy(photoSize = photoSize) }
          .sortedByDescending { it.uploadedOn }

        val isEndReached = request()?.isEnd ?: false

        setState {
          copy(
            isEndReached = isEndReached,
            receivedPhotosRequest = request,
            receivedPhotos = newReceivedPhotos
          )
        }
      }
    }
  }

  fun onReceivePhotosEvent(event: ReceivedPhotosFragmentEvent.ReceivePhotosEvent) {
    when (event) {
      is ReceivedPhotosFragmentEvent.ReceivePhotosEvent.PhotosReceived -> {
        withState { state ->
          val updatedPhotos = state.receivedPhotos.toMutableList()

          for (receivedPhoto in event.receivedPhotos) {
            val photoIndex = state.receivedPhotos
              .indexOfFirst { it.receivedPhotoName == receivedPhoto.receivedPhotoName }

            if (photoIndex != -1) {
              updatedPhotos[photoIndex] = receivedPhoto
            } else {
              updatedPhotos.add(receivedPhoto)
            }
          }

          val updatedSortedPhotos = updatedPhotos
            .sortedByDescending { it.uploadedOn }

          setState {
            copy(
              receivedPhotosRequest = Success(Paged(updatedSortedPhotos)),
              receivedPhotos = updatedSortedPhotos
            )
          }
        }
      }
      is ReceivedPhotosFragmentEvent.ReceivePhotosEvent.NoPhotosReceived -> {
        //do nothing?
      }
      is ReceivedPhotosFragmentEvent.ReceivePhotosEvent.OnFailed -> {
        event.error.printStackTrace()
        Timber.tag(TAG).d("Error while trying to receive photos: (${event.error.message})")
      }
    }.safe
  }

  fun clear() {
    onCleared()
  }

  override fun onCleared() {
    super.onCleared()

    compositeDisposable.dispose()
    job.cancel()
  }

  sealed class ActorAction {
    object LoadReceivedPhotos : ActorAction()
    class ResetState(val clearCache: Boolean) : ActorAction()
    class SwapPhotoAndMap(val receivedPhotoName: String) : ActorAction()
    object FetchFreshPhotos : ActorAction()
    class OnNewPhotoReceived(val photoExchangedData: PhotoExchangedData) : ActorAction()
  }

  companion object : MvRxViewModelFactory<ReceivedPhotosFragmentState> {
    override fun create(
      activity: FragmentActivity,
      state: ReceivedPhotosFragmentState
    ): BaseMvRxViewModel<ReceivedPhotosFragmentState> {
      return (activity as PhotosActivity).viewModel.receivedPhotosFragmentViewModel
    }
  }
}