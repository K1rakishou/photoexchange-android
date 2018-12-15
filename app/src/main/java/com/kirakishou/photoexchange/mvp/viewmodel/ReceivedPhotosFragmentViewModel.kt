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
import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.extension.filterDuplicatesWith
import com.kirakishou.photoexchange.helper.intercom.event.GalleryFragmentEvent
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.interactors.FavouritePhotoUseCase
import com.kirakishou.photoexchange.interactors.ReportPhotoUseCase
import com.kirakishou.photoexchange.mvp.model.PhotoExchangedData
import com.kirakishou.photoexchange.mvp.model.photo.PhotoAdditionalInfo
import com.kirakishou.photoexchange.mvp.viewmodel.state.ReceivedPhotosFragmentState
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.fragment.GalleryFragment
import com.kirakishou.photoexchange.ui.fragment.UploadedPhotosFragment
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

class ReceivedPhotosFragmentViewModel(
  initialState: ReceivedPhotosFragmentState,
  private val intercom: PhotosActivityViewModelIntercom,
  private val timeUtils: TimeUtils,
  private val receivedPhotosRepository: ReceivedPhotosRepository,
  private val getReceivedPhotosUseCase: GetReceivedPhotosUseCase,
  private val favouritePhotoUseCase: FavouritePhotoUseCase,
  private val reportPhotoUseCase: ReportPhotoUseCase,
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
          is ActorAction.LoadReceivedPhotos -> loadReceivedPhotosInternal(action.forced)
          is ActorAction.ResetState -> resetStateInternal(action.clearCache)
          is ActorAction.SwapPhotoAndMap -> swapPhotoAndMapInternal(action.receivedPhotoName)
          is ActorAction.OnNewPhotoReceived -> onNewPhotoReceivedInternal(action.photoExchangedData)
          is ActorAction.RemovePhoto -> removePhotoInternal(action.photoName)
          is ActorAction.FavouritePhoto -> favouritePhotoInternal(action.photoName)
          is ActorAction.ReportPhoto -> reportPhotoInternal(action.photoName)
          is ActorAction.OnPhotoReported -> onPhotoReportedInternal(action.photoName, action.isReported)
          is ActorAction.OnPhotoFavourited -> {
            onPhotoFavouritedInternal(action.photoName, action.isFavourited, action.favouritesCount)
          }
        }.safe
      }
    }
  }

  fun loadReceivedPhotos(forced: Boolean) {
    launch { viewModelActor.send(ActorAction.LoadReceivedPhotos(forced)) }
  }

  fun resetState(clearCache: Boolean) {
    launch { viewModelActor.send(ActorAction.ResetState(clearCache)) }
  }

  fun swapPhotoAndMap(receivedPhotoName: String) {
    launch { viewModelActor.send(ActorAction.SwapPhotoAndMap(receivedPhotoName)) }
  }

  fun onNewPhotoReceived(photoExchangedData: PhotoExchangedData) {
    launch { viewModelActor.send(ActorAction.OnNewPhotoReceived(photoExchangedData)) }
  }

  fun removePhoto(photoName: String) {
    launch { viewModelActor.send(ActorAction.RemovePhoto(photoName)) }
  }

  fun favouritePhoto(receivedPhotoName: String) {
    launch { viewModelActor.send(ActorAction.FavouritePhoto(receivedPhotoName)) }
  }

  fun reportPhotos(receivedPhotoName: String) {
    launch { viewModelActor.send(ActorAction.ReportPhoto(receivedPhotoName)) }
  }

  fun onPhotoReported(photoName: String, isReported: Boolean) {
    launch { viewModelActor.send(ActorAction.OnPhotoReported(photoName, isReported)) }
  }

  fun onPhotoFavourited(
    photoName: String,
    isFavourited: Boolean,
    favouritesCount: Long
  ) {
    launch {
      viewModelActor.send(ActorAction.OnPhotoFavourited(photoName, isFavourited, favouritesCount))
    }
  }

  private fun onPhotoFavouritedInternal(
    photoName: String,
    isFavourited: Boolean,
    favouritesCount: Long
  ) {
    withState { state ->
      launch {
        val photoIndex = state.receivedPhotos.indexOfFirst { it.receivedPhotoName == photoName }
        if (photoIndex == -1) {
          return@launch
        }

        val updatedPhotos = state.receivedPhotos.toMutableList()
        val receivedPhoto = updatedPhotos[photoIndex]

        val updatedPhotoInfo = updatedPhotos[photoIndex].photoAdditionalInfo
          .copy(
            isFavourited = isFavourited,
            favouritesCount = favouritesCount
          )

        updatedPhotos[photoIndex] = receivedPhoto.copy(
          photoAdditionalInfo = updatedPhotoInfo
        )

        setState { copy(receivedPhotos = updatedPhotos) }
      }
    }
  }

  private fun onPhotoReportedInternal(photoName: String, isReported: Boolean) {
    withState { state ->
      launch {
        val photoIndex = state.receivedPhotos.indexOfFirst { it.receivedPhotoName == photoName }
        if (photoIndex == -1) {
          return@launch
        }

        val updatedPhotos = state.receivedPhotos.toMutableList()
        val receivedPhoto = updatedPhotos[photoIndex]

        val updatedPhotoInfo = updatedPhotos[photoIndex].photoAdditionalInfo
          .copy(isReported = isReported)

        updatedPhotos[photoIndex] = receivedPhoto
          .copy(photoAdditionalInfo = updatedPhotoInfo)

        setState { copy(receivedPhotos = updatedPhotos) }
      }
    }
  }

  //TODO: should user be allowed to report his own photos?
  private fun reportPhotoInternal(photoName: String) {
    fun updateIsPhotoReported(state: ReceivedPhotosFragmentState, photoName: String) {
      if (state.reportedPhotos.contains(photoName)) {
        setState { copy(reportedPhotos = state.reportedPhotos - photoName) }
      } else {
        setState { copy(reportedPhotos = state.reportedPhotos + photoName) }
      }
    }

    fun onFail(state: ReceivedPhotosFragmentState, photoName: String, error: Throwable) {
      updateIsPhotoReported(state, photoName)

      val message = "Could not report photo, error is \"${error.message ?: "Unknown error"}\""
      intercom.tell<PhotosActivity>()
        .to(PhotosActivityEvent.ShowToast(message))
    }

    withState { state ->
      launch {
        updateIsPhotoReported(state, photoName)

        val reportResult = try {
          reportPhotoUseCase.reportPhoto(photoName)
        } catch (error: Throwable) {
          Timber.tag(TAG).e(error)

          onFail(state, photoName, error)
          return@launch
        }

        val photoIndex = state.receivedPhotos.indexOfFirst { it.receivedPhotoName == photoName }
        if (photoIndex == -1) {
          return@launch
        }

        val updatedPhotos = state.receivedPhotos.toMutableList()
        val galleryPhoto = updatedPhotos[photoIndex]

        val updatedPhotoInfo = updatedPhotos[photoIndex].photoAdditionalInfo
          .copy(isReported = reportResult)

        updatedPhotos[photoIndex] = galleryPhoto
          .copy(photoAdditionalInfo = updatedPhotoInfo)

        //only show delete photo dialog when we reporting a photo and not removing the report
        if (reportResult) {
          intercom.tell<PhotosActivity>()
            .to(PhotosActivityEvent.ShowDeletePhotoDialog(photoName))
        }

        //notify GalleryFragment that a photo has been reported
        intercom.tell<GalleryFragment>()
          .that(GalleryFragmentEvent.GeneralEvents.PhotoReported(photoName, reportResult))

        setState { copy(receivedPhotos = updatedPhotos) }
      }
    }
  }

  //TODO: should user be allowed to favourite his own photos?
  private fun favouritePhotoInternal(photoName: String) {
    fun updateIsPhotoFavourited(state: ReceivedPhotosFragmentState, photoName: String) {
      if (state.favouritedPhotos.contains(photoName)) {
        setState { copy(favouritedPhotos = state.favouritedPhotos - photoName) }
      } else {
        setState { copy(favouritedPhotos = state.favouritedPhotos + photoName) }
      }
    }

    fun onFail(state: ReceivedPhotosFragmentState, photoName: String, error: Throwable) {
      updateIsPhotoFavourited(state, photoName)

      val message = "Could not favourite photo, error is \"${error.message ?: "Unknown error"}\""
      intercom.tell<PhotosActivity>()
        .to(PhotosActivityEvent.ShowToast(message))
    }

    withState { state ->
      launch {
        updateIsPhotoFavourited(state, photoName)

        val favouriteResult = try {
          favouritePhotoUseCase.favouritePhoto(photoName)
        } catch (error: Throwable) {
          Timber.tag(TAG).e(error)

          onFail(state, photoName, error)
          return@launch
        }

        val photoIndex = state.receivedPhotos.indexOfFirst { it.receivedPhotoName == photoName }
        if (photoIndex == -1) {
          return@launch
        }

        val updatedPhotos = state.receivedPhotos.toMutableList()
        val galleryPhoto = updatedPhotos[photoIndex]

        val updatedPhotoInfo = updatedPhotos[photoIndex].photoAdditionalInfo
          .copy(
            isFavourited = favouriteResult.isFavourited,
            favouritesCount = favouriteResult.favouritesCount
          )

        updatedPhotos[photoIndex] = galleryPhoto.copy(
          photoAdditionalInfo = updatedPhotoInfo
        )

        //notify GalleryFragment that a photo has been favourited
        intercom.tell<GalleryFragment>()
          .that(GalleryFragmentEvent.GeneralEvents.PhotoFavourited(
            photoName, favouriteResult.isFavourited, favouriteResult.favouritesCount
          ))

        setState { copy(receivedPhotos = updatedPhotos) }
      }
    }
  }

  private fun removePhotoInternal(photoName: String) {
    withState { state ->
      val photoIndex = state.receivedPhotos.indexOfFirst { it.receivedPhotoName == photoName }
      if (photoIndex == -1) {
        //nothing to remove
        return@withState
      }

      val updatedPhotos = state.receivedPhotos.toMutableList().apply {
        removeAt(photoIndex)
      }

      setState { copy(receivedPhotos = updatedPhotos) }
    }
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
        LonLat(
          photoExchangedData.lon,
          photoExchangedData.lat
        ),
        photoExchangedData.uploadedOn,
        PhotoAdditionalInfo.empty(photoExchangedData.receivedPhotoName),
        true,
        photoSize
      )

      //TODO: fetch photo additional info here

      val updatedPhotos = state.receivedPhotos.toMutableList() + newPhoto
      val sortedPhotos = updatedPhotos
        .sortedByDescending { it.uploadedOn }

      //show a snackbar telling user that we got a photo
      intercom.tell<PhotosActivity>()
        .that(PhotosActivityEvent.OnNewPhotoReceived)

      setState { copy(receivedPhotos = sortedPhotos) }
    }
  }

  private fun swapPhotoAndMapInternal(receivedPhotoName: String) {
    withState { state ->
      val photoIndex = state.receivedPhotos.indexOfFirst { it.receivedPhotoName == receivedPhotoName }
      if (photoIndex == -1) {
        return@withState
      }

      if (state.receivedPhotos[photoIndex].lonLat.isEmpty()) {
        intercom.tell<PhotosActivity>().to(PhotosActivityEvent
          .ShowToast("Photo was sent anonymously"))
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

      //to avoid "Your reducer must be pure!" exceptions
      val newState = ReceivedPhotosFragmentState()
      setState { newState }

      viewModelActor.send(ActorAction.LoadReceivedPhotos(false))
    }
  }

  private suspend fun loadReceivedPhotosInternal(forced: Boolean) {
    withState { state ->
      if (state.receivedPhotosRequest is Loading) {
        return@withState
      }

      launch {
        val firstUploadedOn = state.receivedPhotos
          .firstOrNull()
          ?.uploadedOn
          ?: -1L

        val lastUploadedOn = state.receivedPhotos
          .lastOrNull()
          ?.uploadedOn
          ?: -1L

        //to avoid "Your reducer must be pure!" exceptions
        val receivedPhotosRequest = Loading<Paged<ReceivedPhoto>>()
        setState { copy(receivedPhotosRequest = receivedPhotosRequest) }

        val request = try {
          val receivedPhotos = getReceivedPhotosUseCase.loadPageOfPhotos(
            forced,
            firstUploadedOn,
            lastUploadedOn,
            photosPerPage
          )

          intercom.tell<UploadedPhotosFragment>()
            .to(UploadedPhotosFragmentEvent.ReceivePhotosEvent.PhotosReceived(receivedPhotos.page.map { it }))

          Success(receivedPhotos)
        } catch (error: Throwable) {
          Timber.tag(TAG).e(error)
          Fail<Paged<ReceivedPhoto>>(error)
        }

        val newPhotos = (request()?.page ?: emptyList())
        val newReceivedPhotos = state.receivedPhotos
          .filterDuplicatesWith(newPhotos) { it.uploadedPhotoName }
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

          //to avoid "Your reducer must be pure!" exceptions
          val request = Success(Paged(updatedSortedPhotos))

          setState {
            copy(
              receivedPhotosRequest = request,
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

    compositeDisposable.clear()
    job.cancelChildren()
  }

  sealed class ActorAction {
    class LoadReceivedPhotos(val forced: Boolean) : ActorAction()
    class ResetState(val clearCache: Boolean) : ActorAction()
    class SwapPhotoAndMap(val receivedPhotoName: String) : ActorAction()
    class OnNewPhotoReceived(val photoExchangedData: PhotoExchangedData) : ActorAction()
    class RemovePhoto(val photoName: String) : ActorAction()
    class ReportPhoto(val photoName: String) : ActorAction()
    class FavouritePhoto(val photoName: String) : ActorAction()
    class OnPhotoReported(val photoName: String,
                          val isReported: Boolean) : ActorAction()
    class OnPhotoFavourited(val photoName: String,
                            val isFavourited: Boolean,
                            val favouritesCount: Long) : ActorAction()
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