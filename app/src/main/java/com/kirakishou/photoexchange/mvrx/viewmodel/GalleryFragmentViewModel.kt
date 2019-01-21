package com.kirakishou.photoexchange.mvrx.viewmodel

import androidx.fragment.app.FragmentActivity
import com.airbnb.mvrx.*
import com.kirakishou.photoexchange.helper.Constants
import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.GalleryPhotosRepository
import com.kirakishou.photoexchange.helper.extension.filterDuplicatesWith
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.intercom.event.ReceivedPhotosFragmentEvent
import com.kirakishou.photoexchange.usecases.FavouritePhotoUseCase
import com.kirakishou.photoexchange.usecases.GetFreshPhotosUseCase
import com.kirakishou.photoexchange.usecases.GetGalleryPhotosUseCase
import com.kirakishou.photoexchange.usecases.ReportPhotoUseCase
import com.kirakishou.photoexchange.mvrx.model.PhotoSize
import com.kirakishou.photoexchange.mvrx.model.photo.GalleryPhoto
import com.kirakishou.photoexchange.mvrx.viewmodel.state.GalleryFragmentState
import com.kirakishou.photoexchange.mvrx.viewmodel.state.UpdateStateResult
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

open class GalleryFragmentViewModel(
  initialState: GalleryFragmentState,
  private val intercom: PhotosActivityViewModelIntercom,
  private val galleryPhotosRepository: GalleryPhotosRepository,
  private val getGalleryPhotosUseCase: GetGalleryPhotosUseCase,
  private val favouritePhotoUseCase: FavouritePhotoUseCase,
  private val getFreshPhotosUseCase: GetFreshPhotosUseCase,
  private val reportPhotoUseCase: ReportPhotoUseCase,
  private val dispatchersProvider: DispatchersProvider
) : MyBaseMvRxViewModel<GalleryFragmentState>(initialState), CoroutineScope {
  private val TAG = "GalleryFragmentViewModel"

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
          is ActorAction.LoadGalleryPhotos -> loadGalleryPhotosInternal(action.forced)
          is ActorAction.ResetState -> resetStateInternal(action.clearCache)
          is ActorAction.SwapPhotoAndMap -> swapPhotoAndMapInternal(action.galleryPhotoName)
          is ActorAction.FavouritePhoto -> favouritePhotoInternal(action.galleryPhotoName)
          is ActorAction.ReportPhoto -> reportPhotoInternal(action.galleryPhotoName)
          is ActorAction.RemovePhoto -> removePhotoInternal(action.photoName)
          is ActorAction.OnPhotoReported -> onPhotoReportedInternal(
            action.photoName,
            action.isReported
          )
          is ActorAction.OnPhotoFavourited -> {
            onPhotoFavouritedInternal(action.photoName, action.isFavourited, action.favouritesCount)
          }
          ActorAction.CheckFreshPhotos -> checkFreshPhotosInternal()
        }.safe
      }
    }
  }

  //resets everything to default state
  fun resetState(clearCache: Boolean) {
    launch { viewModelActor.send(ActorAction.ResetState(clearCache)) }
  }

  //load gallery photos page by page
  fun loadGalleryPhotos(forced: Boolean) {
    launch { viewModelActor.send(ActorAction.LoadGalleryPhotos(forced)) }
  }

  //switch between map and photo
  fun swapPhotoAndMap(photoName: String) {
    launch { viewModelActor.send(ActorAction.SwapPhotoAndMap(photoName)) }
  }

  fun favouritePhoto(photoName: String) {
    launch { viewModelActor.send(ActorAction.FavouritePhoto(photoName)) }
  }

  fun reportPhotos(photoName: String) {
    launch { viewModelActor.send(ActorAction.ReportPhoto(photoName)) }
  }

  fun removePhoto(photoName: String) {
    launch { viewModelActor.send(ActorAction.RemovePhoto(photoName)) }
  }

  //called when user reports photo from ReceivedPhotosFragment
  fun onPhotoReported(photoName: String, isReported: Boolean) {
    launch { viewModelActor.send(ActorAction.OnPhotoReported(photoName, isReported)) }
  }

  //called when user favourites photo from ReceivedPhotosFragment
  fun onPhotoFavourited(photoName: String, isFavourited: Boolean, favouritesCount: Long) {
    launch {
      viewModelActor.send(
        ActorAction.OnPhotoFavourited(
          photoName,
          isFavourited,
          favouritesCount
        )
      )
    }
  }

  //check whether someone else has uploaded a new public photo (gallery photo)
  fun checkFreshPhotos() {
    launch { viewModelActor.send(ActorAction.CheckFreshPhotos) }
  }

  private suspend fun checkFreshPhotosInternal() {
    suspendWithState { state ->
      //do not run the request if we are in the failed state
      if (state.galleryPhotosRequest is Fail) {
        return@suspendWithState
      }

      //do not run the request if it is already running
      if (state.checkForFreshPhotosRequest is Loading) {
        return@suspendWithState
      }

      val firstUploadedOn = state.galleryPhotos
        .firstOrNull()
        ?.uploadedOn

      if (firstUploadedOn == null) {
        //no photos
        return@suspendWithState
      }

      val loadingState = Loading<Unit>()
      setState { copy(checkForFreshPhotosRequest = loadingState) }

      val freshPhotosRequest = try {
        Success(getFreshPhotosUseCase.getFreshGalleryPhotos(false, firstUploadedOn))
      } catch (error: Throwable) {
        Timber.tag(TAG).e(error, "Error while trying to check fresh gallery photos")
        setState { copy(checkForFreshPhotosRequest = Uninitialized) }
        //TODO: notify user about this error?
        return@suspendWithState
      }

      val freshPhotos = freshPhotosRequest() ?: emptyList()
      if (freshPhotos.isEmpty()) {
        setState { copy(checkForFreshPhotosRequest = Uninitialized) }
        return@suspendWithState
      }

      val newPhotosCount = freshPhotos.count { it.uploadedOn > firstUploadedOn }

      //if there are any fresh photos - show snackbar
      if (newPhotosCount > 0) {
        intercom.tell<PhotosActivity>().to(PhotosActivityEvent.OnNewGalleryPhotos(newPhotosCount))
      }

      val newGalleryPhotos = state.galleryPhotos
        .filterDuplicatesWith(freshPhotos) { it.photoName }
        .map { galleryPhoto -> galleryPhoto.copy(photoSize = photoSize) }
        .sortedByDescending { it.uploadedOn }

      setState {
        copy(
          galleryPhotos = newGalleryPhotos,
          checkForFreshPhotosRequest = Uninitialized
        )
      }
    }
  }

  private suspend fun onPhotoFavouritedInternal(photoName: String, isFavourited: Boolean, favouritesCount: Long) {
    suspendWithState { state ->
      val updateResult = state.onPhotoFavourited(
        photoName,
        isFavourited,
        favouritesCount
      )

      if (updateResult is UpdateStateResult.Update) {
        setState { copy(galleryPhotos = updateResult.update) }
      }
    }
  }

  private suspend fun onPhotoReportedInternal(photoName: String, isReported: Boolean) {
    suspendWithState { state ->
      val updateResult = state.onPhotoReported(
        photoName,
        isReported
      )

      if (updateResult is UpdateStateResult.Update) {
        setState { copy(galleryPhotos = updateResult.update) }
      }
    }
  }

  private suspend fun removePhotoInternal(photoName: String) {
    suspendWithState { state ->
      val updateResult = state.removePhoto(photoName)

      if (updateResult is UpdateStateResult.Update) {
        setState { copy(galleryPhotos = updateResult.update) }
      }
    }
  }

  //TODO: should user be allowed to report his own photos?
  private suspend fun reportPhotoInternal(photoName: String) {
    fun updateIsPhotoReported(state: GalleryFragmentState, photoName: String) {
      if (state.reportedPhotos.contains(photoName)) {
        setState { copy(reportedPhotos = state.reportedPhotos - photoName) }
      } else {
        setState { copy(reportedPhotos = state.reportedPhotos + photoName) }
      }
    }

    fun onFail(state: GalleryFragmentState, photoName: String, error: Throwable) {
      updateIsPhotoReported(state, photoName)

      val message = "Could not report photo, error is \"${error.message ?: "Unknown error"}\""
      intercom.tell<PhotosActivity>()
        .to(PhotosActivityEvent.ShowToast(message))
    }

    suspendWithState { state ->
      updateIsPhotoReported(state, photoName)

      val reportResult = try {
        reportPhotoUseCase.reportPhoto(photoName)
      } catch (error: Throwable) {
        Timber.tag(TAG).e(error)

        onFail(state, photoName, error)
        return@suspendWithState
      }

      val updateResult = state.reportPhoto(photoName, reportResult)

      //only show delete photo dialog when we reporting a photo and not removing the report
      if (reportResult) {
        intercom.tell<PhotosActivity>()
          .to(PhotosActivityEvent.ShowDeletePhotoDialog(photoName))
      }

      //notify ReceivedPhotosFragment that a photo has been reported
      intercom.tell<ReceivedPhotosFragment>()
        .that(ReceivedPhotosFragmentEvent.GeneralEvents.PhotoReported(photoName, reportResult))

      if (updateResult is UpdateStateResult.Update) {
        setState { copy(galleryPhotos = updateResult.update) }
      }
    }
  }

  //TODO: should user be allowed to favourite his own photos?
  private suspend fun favouritePhotoInternal(photoName: String) {
    fun updateIsPhotoFavourited(state: GalleryFragmentState, photoName: String) {
      if (state.favouritedPhotos.contains(photoName)) {
        setState { copy(favouritedPhotos = state.favouritedPhotos - photoName) }
      } else {
        setState { copy(favouritedPhotos = state.favouritedPhotos + photoName) }
      }
    }

    fun onFail(state: GalleryFragmentState, photoName: String, error: Throwable) {
      updateIsPhotoFavourited(state, photoName)

      val message = "Could not favourite photo, error is \"${error.message ?: "Unknown error"}\""
      intercom.tell<PhotosActivity>()
        .to(PhotosActivityEvent.ShowToast(message))
    }

    suspendWithState { state ->
      updateIsPhotoFavourited(state, photoName)

      val favouriteResult = try {
        favouritePhotoUseCase.favouritePhoto(photoName)
      } catch (error: Throwable) {
        Timber.tag(TAG).e(error)

        onFail(state, photoName, error)
        return@suspendWithState
      }

      val updateResult = state.favouritePhoto(
        photoName,
        favouriteResult.isFavourited,
        favouriteResult.favouritesCount
      )

      //notify ReceivedPhotosFragment that a photo has been favourited
      intercom.tell<ReceivedPhotosFragment>()
        .that(
          ReceivedPhotosFragmentEvent.GeneralEvents.PhotoFavourited(
            photoName, favouriteResult.isFavourited, favouriteResult.favouritesCount
          )
        )

      if (updateResult is UpdateStateResult.Update) {
        setState { copy(galleryPhotos = updateResult.update) }
      }
    }
  }

  private suspend fun swapPhotoAndMapInternal(photoName: String) {
    suspendWithState { state ->
      val updateResult = state.swapPhotoAndMap(photoName)

      when (updateResult) {
        is UpdateStateResult.Update -> {
          setState { copy(galleryPhotos = updateResult.update) }
        }
        is UpdateStateResult.SendIntercom -> {
          intercom.tell<PhotosActivity>().to(
            PhotosActivityEvent
              .ShowToast("Photo was sent anonymously")
          )
        }
        is UpdateStateResult.NothingToUpdate -> {
        }
      }.safe
    }
  }

  private suspend fun resetStateInternal(clearCache: Boolean) {
    suspendWithState {
      if (clearCache) {
        galleryPhotosRepository.deleteAll()
      }

      //to avoid "Your reducer must be pure!" exceptions
      val newState = GalleryFragmentState()
      setState { newState }

      viewModelActor.send(ActorAction.LoadGalleryPhotos(false))
    }
  }

  private suspend fun loadGalleryPhotosInternal(forced: Boolean) {
    suspendWithState { state ->
      if (state.galleryPhotosRequest is Loading) {
        return@suspendWithState
      }

      if (state.isEndReached) {
        return@suspendWithState
      }

      //to avoid "Your reducer must be pure!" exceptions
      val galleryPhotosRequest = Loading<Paged<GalleryPhoto>>()
      setState { copy(galleryPhotosRequest = galleryPhotosRequest) }

      val firstUploadedOn = state.galleryPhotos
        .firstOrNull()
        ?.uploadedOn

      val lastUploadedOn = state.galleryPhotos
        .lastOrNull()
        ?.uploadedOn

      val request = try {
        val photos = getGalleryPhotosUseCase.loadPageOfPhotos(
          forced,
          firstUploadedOn,
          lastUploadedOn,
          photosPerPage
        )

        Success(photos)
      } catch (error: Throwable) {
        Timber.tag(TAG).e(error)
        Fail<Paged<GalleryPhoto>>(error)
      }

      val newPhotos = (request()?.page ?: emptyList())
      if (firstUploadedOn != null) {
        val newPhotosCount = newPhotos.count { it.uploadedOn > firstUploadedOn }

        //if there are any fresh photos - show snackbar
        if (newPhotosCount > 0) {
          intercom.tell<PhotosActivity>().to(PhotosActivityEvent.OnNewGalleryPhotos(newPhotosCount))
        }
      }

      val newGalleryPhotos = state.galleryPhotos
        .filterDuplicatesWith(newPhotos) { it.photoName }
        .map { galleryPhoto -> galleryPhoto.copy(photoSize = photoSize) }
        .sortedByDescending { it.uploadedOn }

      val isEndReached = request()?.isEnd ?: false

      setState {
        copy(
          isEndReached = isEndReached,
          galleryPhotosRequest = request,
          galleryPhotos = newGalleryPhotos
        )
      }
    }
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
    class LoadGalleryPhotos(val forced: Boolean) : ActorAction()
    class ResetState(val clearCache: Boolean) : ActorAction()
    class SwapPhotoAndMap(val galleryPhotoName: String) : ActorAction()
    class ReportPhoto(val galleryPhotoName: String) : ActorAction()
    class FavouritePhoto(val galleryPhotoName: String) : ActorAction()
    class RemovePhoto(val photoName: String) : ActorAction()
    class OnPhotoReported(val photoName: String,
                          val isReported: Boolean) : ActorAction()

    class OnPhotoFavourited(val photoName: String,
                            val isFavourited: Boolean,
                            val favouritesCount: Long) : ActorAction()

    object CheckFreshPhotos : ActorAction()
  }

  companion object : MvRxViewModelFactory<GalleryFragmentState> {
    override fun create(
      activity: FragmentActivity,
      state: GalleryFragmentState
    ): BaseMvRxViewModel<GalleryFragmentState> {
      return (activity as PhotosActivity).viewModel.galleryFragmentViewModel
    }
  }
}