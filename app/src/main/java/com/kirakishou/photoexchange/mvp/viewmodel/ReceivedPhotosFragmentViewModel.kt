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
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
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
        //photos is already shown
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
      val sortedPhotos = updatedPhotos.sortedByDescending { it.uploadedOn }

      intercom.tell<PhotosActivity>().to(PhotosActivityEvent.OnNewPhotoReceived)
      setState { copy(receivedPhotos = sortedPhotos) }
    }
  }

  private fun fetchFreshPhotosInternal() {
    //TODO
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

        val request = try {
          receivedPhotosRepository.deleteOldPhotos()

          val result = getReceivedPhotosUseCase.loadPageOfPhotos(lastUploadedOn, photosPerPage)
          if (result is Either.Error) {
            throw result.error
          }

          result as Either.Value

          intercom.tell<UploadedPhotosFragment>()
            .to(UploadedPhotosFragmentEvent.ReceivePhotosEvent.PhotosReceived(result.value))

          val receivedPhotos = result.value.also { receivedPhotos ->
            receivedPhotos.map { receivedPhoto -> receivedPhoto.copy(photoSize = photoSize) }
          }

          Success(receivedPhotos)
        } catch (error: Throwable) {
          Fail<List<ReceivedPhoto>>(error)
        }

        val newReceivedPhotos = request() ?: emptyList()
        val isEndReached = newReceivedPhotos.size < photosPerPage

        setState {
          copy(
            isEndReached = isEndReached,
            receivedPhotosRequest = request,
            receivedPhotos = state.receivedPhotos + newReceivedPhotos
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
              receivedPhotosRequest = Success(updatedSortedPhotos),
              receivedPhotos = updatedSortedPhotos
            )
          }
        }
      }
      is ReceivedPhotosFragmentEvent.ReceivePhotosEvent.NoPhotosReceived -> {
        //TODO
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