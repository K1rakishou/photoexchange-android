package com.kirakishou.photoexchange.mvp.viewmodel

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
import com.kirakishou.photoexchange.interactors.FavouritePhotoUseCase
import com.kirakishou.photoexchange.interactors.GetGalleryPhotosUseCase
import com.kirakishou.photoexchange.interactors.ReportPhotoUseCase
import com.kirakishou.photoexchange.mvp.model.PhotoSize
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhoto
import com.kirakishou.photoexchange.mvp.viewmodel.state.GalleryFragmentState
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

class GalleryFragmentViewModel(
  initialState: GalleryFragmentState,
  private val intercom: PhotosActivityViewModelIntercom,
  private val galleryPhotosRepository: GalleryPhotosRepository,
  private val getGalleryPhotosUseCase: GetGalleryPhotosUseCase,
  private val favouritePhotoUseCase: FavouritePhotoUseCase,
  private val reportPhotoUseCase: ReportPhotoUseCase,
  private val dispatchersProvider: DispatchersProvider
) : BaseMvRxViewModel<GalleryFragmentState>(initialState), CoroutineScope {
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
          is ActorAction.OnPhotoReported -> onPhotoReportedInternal(action.photoName, action.isReported)
          is ActorAction.OnPhotoFavourited -> {
            onPhotoFavouritedInternal(action.photoName, action.isFavourited, action.favouritesCount)
          }
        }.safe
      }
    }
  }

  fun resetState(clearCache: Boolean) {
    launch { viewModelActor.send(ActorAction.ResetState(clearCache)) }
  }

  fun loadGalleryPhotos(forced: Boolean) {
    launch { viewModelActor.send(ActorAction.LoadGalleryPhotos(forced)) }
  }

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
        val photoIndex = state.galleryPhotos.indexOfFirst { it.photoName == photoName }
        if (photoIndex == -1) {
          return@launch
        }

        val updatedPhotos = state.galleryPhotos.toMutableList()
        val galleryPhoto = updatedPhotos[photoIndex]

        val updatedPhotoInfo = updatedPhotos[photoIndex].photoAdditionalInfo
          .copy(
            isFavourited = isFavourited,
            favouritesCount = favouritesCount
          )

        updatedPhotos[photoIndex] = galleryPhoto.copy(
          photoAdditionalInfo = updatedPhotoInfo
        )

        setState { copy(galleryPhotos = updatedPhotos) }
      }
    }
  }

  private fun onPhotoReportedInternal(photoName: String, isReported: Boolean) {
    withState { state ->
      launch {
        val photoIndex = state.galleryPhotos.indexOfFirst { it.photoName == photoName }
        if (photoIndex == -1) {
          return@launch
        }

        val updatedPhotos = state.galleryPhotos.toMutableList()
        val galleryPhoto = updatedPhotos[photoIndex]

        val updatedPhotoInfo = updatedPhotos[photoIndex].photoAdditionalInfo
          .copy(isReported = isReported)

        updatedPhotos[photoIndex] = galleryPhoto
          .copy(photoAdditionalInfo = updatedPhotoInfo)

        setState { copy(galleryPhotos = updatedPhotos) }
      }
    }
  }

  private fun removePhotoInternal(photoName: String) {
    withState { state ->
      val photoIndex = state.galleryPhotos.indexOfFirst { it.photoName == photoName }
      if (photoIndex == -1) {
        //nothing to remove
        return@withState
      }

      val updatedPhotos = state.galleryPhotos.toMutableList().apply {
        removeAt(photoIndex)
      }

      setState { copy(galleryPhotos = updatedPhotos) }
    }
  }

  //TODO: should user be allowed to report his own photos?
  private fun reportPhotoInternal(photoName: String) {
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

        val photoIndex = state.galleryPhotos.indexOfFirst { it.photoName == photoName }
        if (photoIndex == -1) {
          return@launch
        }

        val updatedPhotos = state.galleryPhotos.toMutableList()
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

        //notify ReceivedPhotosFragment that a photo has been reported
        intercom.tell<ReceivedPhotosFragment>()
          .that(ReceivedPhotosFragmentEvent.GeneralEvents.PhotoReported(photoName, reportResult))

        setState { copy(galleryPhotos = updatedPhotos) }
      }
    }
  }

  //TODO: should user be allowed to favourite his own photos?
  private fun favouritePhotoInternal(photoName: String) {
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

        val photoIndex = state.galleryPhotos.indexOfFirst { it.photoName == photoName }
        if (photoIndex == -1) {
          return@launch
        }

        val updatedPhotos = state.galleryPhotos.toMutableList()
        val galleryPhoto = updatedPhotos[photoIndex]

        val updatedPhotoInfo = updatedPhotos[photoIndex].photoAdditionalInfo
          .copy(
            isFavourited = favouriteResult.isFavourited,
            favouritesCount = favouriteResult.favouritesCount
          )

        updatedPhotos[photoIndex] = galleryPhoto.copy(
          photoAdditionalInfo = updatedPhotoInfo
        )

        //notify ReceivedPhotosFragment that a photo has been favourited
        intercom.tell<ReceivedPhotosFragment>()
          .that(ReceivedPhotosFragmentEvent.GeneralEvents.PhotoFavourited(
            photoName, favouriteResult.isFavourited, favouriteResult.favouritesCount
          ))

        setState { copy(galleryPhotos = updatedPhotos) }
      }
    }
  }

  private fun swapPhotoAndMapInternal(photoName: String) {
    withState { state ->
      val photoIndex = state.galleryPhotos.indexOfFirst { it.photoName == photoName }
      if (photoIndex == -1) {
        return@withState
      }

      if (state.galleryPhotos[photoIndex].lonLat.isEmpty()) {
        intercom.tell<PhotosActivity>().to(PhotosActivityEvent
          .ShowToast("Photo was sent anonymously"))
        return@withState
      }

      val oldShowPhoto = state.galleryPhotos[photoIndex].showPhoto
      val updatedPhoto = state.galleryPhotos[photoIndex]
        .copy(showPhoto = !oldShowPhoto)

      val updatedPhotos = state.galleryPhotos.toMutableList()
      updatedPhotos[photoIndex] = updatedPhoto

      setState { copy(galleryPhotos = updatedPhotos) }
    }
  }

  private fun resetStateInternal(clearCache: Boolean) {
    launch {
      if (clearCache) {
        galleryPhotosRepository.deleteAll()
      }

      setState { GalleryFragmentState() }
      viewModelActor.send(ActorAction.LoadGalleryPhotos(false))
    }
  }

  private fun loadGalleryPhotosInternal(forced: Boolean) {
    withState { state ->
      if (state.galleryPhotosRequest is Loading) {
        return@withState
      }

      launch {
        val firstUploadedOn = state.galleryPhotos
          .firstOrNull()
          ?.uploadedOn
          ?: -1L

        val lastUploadedOn = state.galleryPhotos
          .lastOrNull()
          ?.uploadedOn
          ?: -1L

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

        val newPageOfPhotos = (request()?.page ?: emptyList())

        if (firstUploadedOn != -1L) {
          val newPhotosCount = newPageOfPhotos.count { it.uploadedOn > firstUploadedOn }
          if (newPhotosCount > 0) {
            intercom.tell<PhotosActivity>().to(PhotosActivityEvent.OnNewGalleryPhotos(newPhotosCount))
          }
        }

        val newGalleryPhotos = state.galleryPhotos
          .filterDuplicatesWith(newPageOfPhotos) { it.photoName }
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