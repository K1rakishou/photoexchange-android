package com.kirakishou.photoexchange.mvp.viewmodel

import androidx.fragment.app.FragmentActivity
import com.airbnb.mvrx.*
import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.mvp.model.PhotoSize
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.interactors.GetGalleryPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhoto
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.Constants
import com.kirakishou.photoexchange.helper.exception.EmptyUserIdException
import com.kirakishou.photoexchange.helper.intercom.event.GalleryFragmentEvent
import com.kirakishou.photoexchange.interactors.FavouritePhotoUseCase
import com.kirakishou.photoexchange.interactors.ReportPhotoUseCase
import com.kirakishou.photoexchange.mvp.model.FavouritePhotoActionResult
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhotoInfo
import com.kirakishou.photoexchange.mvp.viewmodel.state.GalleryFragmentState
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.fragment.GalleryFragment
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class GalleryFragmentViewModel(
  initialState: GalleryFragmentState,
  private val intercom: PhotosActivityViewModelIntercom,
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
          ActorAction.LoadGalleryPhotos -> loadGalleryPhotosInternal()
          ActorAction.ResetState -> resetStateInternal()
          is ActorAction.SwapPhotoAndMap -> swapPhotoAndMapInternal(action.galleryPhotoName)
          is ActorAction.ReportPhoto -> reportPhotoInternal(action.galleryPhotoName)
          is ActorAction.FavouritePhoto -> favouritePhotoInternal(action.galleryPhotoName)
        }.safe
      }
    }

    loadGalleryPhotos()
  }

  fun resetState() {
    launch { viewModelActor.send(ActorAction.ResetState) }
  }

  fun loadGalleryPhotos() {
    launch { viewModelActor.send(ActorAction.LoadGalleryPhotos) }
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

  private fun reportPhotoInternal(photoName: String) {
    //TODO
  }

  private fun favouritePhotoInternal(photoName: String) {
    withState { state ->
      launch {
        setState { copy(isFavouriteRequestActive = true) }

        val result = try {
          val result = favouritePhotoUseCase.favouritePhoto(photoName)
          if (result is Either.Error)  {
            throw result.error
          }

          Success((result as Either.Value).value)
        } catch (error: Throwable) {
          Fail<FavouritePhotoActionResult>(error)
        }

        if (result is Fail) {
          if (result.error is EmptyUserIdException) {
            val updatedPhotos = state.galleryPhotos
              .map { it.copy(galleryPhotoInfo = GalleryPhotoInfo.empty()) }

            setState {
              copy(
                isFavouriteRequestActive = false,
                galleryPhotos = updatedPhotos
              )
            }

            return@launch
          }

          val message = "Could not favourite photo, error is \"${result.error.message
            ?: "Unknown error"}\""

          setState { copy(isFavouriteRequestActive = false) }

          intercom.tell<GalleryFragment>()
            .to(GalleryFragmentEvent.GeneralEvents.ShowToast(message))
          return@launch
        }

        val photoIndex = state.galleryPhotos
          .indexOfFirst { it.photoName == photoName }
        if (photoIndex == -1) {
          return@launch
        }

        val favouriteResult = result()!!
        val updatedPhotos = state.galleryPhotos.toMutableList()
        val galleryPhoto = updatedPhotos[photoIndex]

        val updatedPhotoInfo = updatedPhotos[photoIndex].galleryPhotoInfo
          .copy(isFavourited = favouriteResult.isFavourited)

        updatedPhotos[photoIndex] = galleryPhoto.copy(
          favouritesCount = favouriteResult.favouritesCount,
          galleryPhotoInfo = updatedPhotoInfo
        )

        setState {
          copy(
            isFavouriteRequestActive = false,
            galleryPhotos = updatedPhotos
          )
        }
      }
    }
  }

  private fun swapPhotoAndMapInternal(photoName: String) {
    withState { state ->
      val photoIndex = state.galleryPhotos.indexOfFirst { it.photoName == photoName }
      if (photoIndex == -1) {
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

  private fun resetStateInternal() {
    setState { GalleryFragmentState() }
    launch { viewModelActor.send(ActorAction.LoadGalleryPhotos) }
  }

  private fun loadGalleryPhotosInternal() {
    withState { state ->
      if (state.galleryPhotosRequest is Loading) {
        return@withState
      }

      launch {
        val lastUploadedOn = state.galleryPhotos
          .lastOrNull()
          ?.uploadedOn
          ?: -1L

        val request = try {
          val result = getGalleryPhotosUseCase.loadPageOfPhotos(lastUploadedOn, photosPerPage)
          if (result is Either.Error) {
            throw result.error
          }

          result as Either.Value
          val galleryPhotos = result.value
            .map { galleryPhoto -> galleryPhoto.copy(photoSize = photoSize) }

          Success(galleryPhotos)
        } catch (error: Throwable) {
          Fail<List<GalleryPhoto>>(error)
        }

        val newGalleryPhotos = request() ?: emptyList()
        val isEndReached = newGalleryPhotos.size < photosPerPage

        setState {
          copy(
            isEndReached = isEndReached,
            galleryPhotosRequest = request,
            galleryPhotos = state.galleryPhotos + newGalleryPhotos
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

    compositeDisposable.dispose()
    job.cancel()
  }

  sealed class ActorAction {
    object LoadGalleryPhotos : ActorAction()
    object ResetState : ActorAction()
    class SwapPhotoAndMap(val galleryPhotoName: String) : ActorAction()
    class ReportPhoto(val galleryPhotoName: String) : ActorAction()
    class FavouritePhoto(val galleryPhotoName: String) : ActorAction()
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