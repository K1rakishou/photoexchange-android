package com.kirakishou.photoexchange.mvrx.viewmodel

import androidx.fragment.app.FragmentActivity
import com.airbnb.mvrx.*
import com.kirakishou.fixmypc.photoexchange.BuildConfig
import com.kirakishou.photoexchange.helper.Constants
import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.extension.filterDuplicatesWith
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.helper.intercom.event.GalleryFragmentEvent
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.intercom.event.ReceivedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.usecases.*
import com.kirakishou.photoexchange.mvrx.model.NewReceivedPhoto
import com.kirakishou.photoexchange.mvrx.model.PhotoSize
import com.kirakishou.photoexchange.mvrx.model.photo.PhotoAdditionalInfo
import com.kirakishou.photoexchange.mvrx.model.photo.ReceivedPhoto
import com.kirakishou.photoexchange.mvrx.viewmodel.state.ReceivedPhotosFragmentState
import com.kirakishou.photoexchange.mvrx.viewmodel.state.UpdateStateResult
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
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

open class ReceivedPhotosFragmentViewModel(
  initialState: ReceivedPhotosFragmentState,
  private val intercom: PhotosActivityViewModelIntercom,
  private val receivedPhotosRepository: ReceivedPhotosRepository,
  private val getReceivedPhotosUseCase: GetReceivedPhotosUseCase,
  private val favouritePhotoUseCase: FavouritePhotoUseCase,
  private val reportPhotoUseCase: ReportPhotoUseCase,
  private val getPhotoAdditionalInfoUseCase: GetPhotoAdditionalInfoUseCase,
  private val getFreshPhotosUseCase: GetFreshPhotosUseCase,
  private val dispatchersProvider: DispatchersProvider
) : MyBaseMvRxViewModel<ReceivedPhotosFragmentState>(
  initialState,
  BuildConfig.DEBUG
), CoroutineScope {
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
          is ActorAction.OnNewPhotosReceived -> onNewPhotoReceivedInternal(action.newReceivedPhotos)
          is ActorAction.RemovePhoto -> removePhotoInternal(action.photoName)
          is ActorAction.FavouritePhoto -> favouritePhotoInternal(action.photoName)
          is ActorAction.ReportPhoto -> reportPhotoInternal(action.photoName)
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

  //loads received photos page by page
  fun loadReceivedPhotos(forced: Boolean) {
    launch { viewModelActor.send(ActorAction.LoadReceivedPhotos(forced)) }
  }

  //resets everything to default state
  fun resetState(clearCache: Boolean) {
    launch { viewModelActor.send(ActorAction.ResetState(clearCache)) }
  }

  //switch between map and photo
  fun swapPhotoAndMap(receivedPhotoName: String) {
    launch { viewModelActor.send(ActorAction.SwapPhotoAndMap(receivedPhotoName)) }
  }

  //called when push notification is received
  fun onNewPhotosReceived(newReceivedPhotos: List<NewReceivedPhoto>) {
    launch { viewModelActor.send(ActorAction.OnNewPhotosReceived(newReceivedPhotos)) }
  }

  //remove photo from the state
  fun removePhoto(photoName: String) {
    launch { viewModelActor.send(ActorAction.RemovePhoto(photoName)) }
  }

  fun favouritePhoto(receivedPhotoName: String) {
    launch { viewModelActor.send(ActorAction.FavouritePhoto(receivedPhotoName)) }
  }

  fun reportPhoto(receivedPhotoName: String) {
    launch { viewModelActor.send(ActorAction.ReportPhoto(receivedPhotoName)) }
  }

  //called when user reports photo from GalleryFragment
  fun onPhotoReported(photoName: String, isReported: Boolean) {
    launch { viewModelActor.send(ActorAction.OnPhotoReported(photoName, isReported)) }
  }

  //called when user favourites photo from GalleryFragment
  fun onPhotoFavourited(photoName: String, isFavourited: Boolean, favouritesCount: Long) {
    launch { viewModelActor.send(ActorAction.OnPhotoFavourited(photoName, isFavourited, favouritesCount)) }
  }

  //check whether we have received a new photo by someone
  fun checkFreshPhotos() {
    launch { viewModelActor.send(ActorAction.CheckFreshPhotos) }
  }

  private suspend fun checkFreshPhotosInternal() {
    suspendWithState { state ->
      //do not run the request if we are in the failed state
      if (state.receivedPhotosRequest is Fail) {
        return@suspendWithState
      }

      //do not run the request if it is already running
      if (state.checkForFreshPhotosRequest is Loading) {
        return@suspendWithState
      }

      val firstUploadedOn = state.receivedPhotos
        .firstOrNull()
        ?.uploadedOn

      if (firstUploadedOn == null) {
        //no photos
        return@suspendWithState
      }

      val loadingState = Loading<Unit>()
      setState { copy(checkForFreshPhotosRequest = loadingState) }

      val freshPhotosRequest = try {
        Success(getFreshPhotosUseCase.getFreshReceivedPhotos(false, firstUploadedOn))
      } catch (error: Throwable) {
        Timber.tag(TAG).e(error, "Error while trying to check fresh received photos")
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
        intercom.tell<PhotosActivity>().to(PhotosActivityEvent.OnNewReceivedPhotos(newPhotosCount))
      }

      val newReceivedPhotos = state.receivedPhotos
        .filterDuplicatesWith(freshPhotos) { it.receivedPhotoName }
        .map { galleryPhoto -> galleryPhoto.copy(photoSize = photoSize) }
        .sortedByDescending { it.uploadedOn }

      setState {
        copy(
          receivedPhotos = newReceivedPhotos,
          checkForFreshPhotosRequest = Uninitialized
        )
      }
    }
  }

  private suspend fun onPhotoFavouritedInternal(photoName: String, isFavourited: Boolean, favouritesCount: Long) {
    suspendWithState { state ->
      val updateResult = state.onPhotoFavourited(photoName, isFavourited, favouritesCount)
      if (updateResult is UpdateStateResult.Update) {
        setState { copy(receivedPhotos = updateResult.update) }
      }
    }
  }

  private suspend fun onPhotoReportedInternal(photoName: String, isReported: Boolean) {
    suspendWithState { state ->
      val updateResult = state.onPhotoReported(photoName, isReported)
      if (updateResult is UpdateStateResult.Update) {
        setState { copy(receivedPhotos = updateResult.update) }
      }
    }
  }

  //TODO: should user be allowed to report his own photos?
  private suspend fun reportPhotoInternal(photoName: String) {
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

      //notify GalleryFragment that a photo has been reported
      intercom.tell<GalleryFragment>()
        .that(GalleryFragmentEvent.GeneralEvents.PhotoReported(photoName, reportResult))

      if (updateResult is UpdateStateResult.Update) {
        setState { copy(receivedPhotos = updateResult.update) }
      }
    }
  }

  //TODO: should user be allowed to favourite his own photos?
  private suspend fun favouritePhotoInternal(photoName: String) {
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

      //notify GalleryFragment that a photo has been favourited
      intercom.tell<GalleryFragment>()
        .that(
          GalleryFragmentEvent.GeneralEvents.PhotoFavourited(
            photoName, favouriteResult.isFavourited, favouriteResult.favouritesCount
          )
        )

      if (updateResult is UpdateStateResult.Update) {
        setState { copy(receivedPhotos = updateResult.update) }
      }
    }
  }

  private suspend fun removePhotoInternal(photoName: String) {
    suspendWithState { state ->
      val updateResult = state.removePhoto(photoName)
      if (updateResult is UpdateStateResult.Update) {
        setState { copy(receivedPhotos = updateResult.update) }
      }
    }
  }

  // FIXME: make faster (use hashSet/Map for filtering already added photos)
  // This function is called every time a new page of uploaded photos is loaded and
  // it is a pretty slow function
  private suspend fun onNewPhotoReceivedInternal(newReceivedPhotos: List<NewReceivedPhoto>) {

    suspend fun updateAdditionalPhotoInfo(
      photoNameListToFetchAdditionalInfo: MutableList<String>,
      updatedPhotos: MutableList<ReceivedPhoto>
    ) {
      val additionalPhotoInfoList = getPhotoAdditionalInfoUseCase.getPhotoAdditionalInfoByPhotoNameList(
        photoNameListToFetchAdditionalInfo
      )

      if (additionalPhotoInfoList == null) {
        return
      }

      for (photoName in photoNameListToFetchAdditionalInfo) {
        val photoIndex = updatedPhotos.indexOfFirst { photo ->
          photo.receivedPhotoName == photoName
        }

        if (photoIndex == -1) {
          continue
        }

        val additionalPhotoInfo = additionalPhotoInfoList.firstOrNull { it.photoName == photoName }

        val photoWithUpdatedAdditionalInfo = updatedPhotos[photoIndex].copy(
          photoAdditionalInfo = additionalPhotoInfo ?: PhotoAdditionalInfo.empty(photoName)
        )

        updatedPhotos.removeAt(photoIndex)
        updatedPhotos.add(photoIndex, photoWithUpdatedAdditionalInfo)
      }
    }

    suspendWithState { state ->
      val updatedPhotos = state.receivedPhotos.toMutableList()
      val photoNameListToFetchAdditionalInfo = mutableListOf<String>()

      for (newReceivedPhoto in newReceivedPhotos) {
        val photoIndex = updatedPhotos.indexOfFirst { receivedPhoto ->
          receivedPhoto.receivedPhotoName == newReceivedPhoto.receivedPhotoName
        }

        if (photoIndex != -1) {
          continue
        }

        val newPhoto = ReceivedPhoto(
          newReceivedPhoto.uploadedPhotoName,
          newReceivedPhoto.receivedPhotoName,
          LonLat(
            newReceivedPhoto.lon,
            newReceivedPhoto.lat
          ),
          newReceivedPhoto.uploadedOn,
          PhotoAdditionalInfo.empty(newReceivedPhoto.receivedPhotoName),
          true,
          photoSize
        )

        updatedPhotos += newPhoto
        photoNameListToFetchAdditionalInfo += newReceivedPhoto.receivedPhotoName
      }

      if (photoNameListToFetchAdditionalInfo.isNotEmpty()) {
        updateAdditionalPhotoInfo(photoNameListToFetchAdditionalInfo, updatedPhotos)
      }

      updatedPhotos.sortByDescending { it.uploadedOn }

      setState { copy(receivedPhotos = updatedPhotos) }
    }
  }

  private suspend fun swapPhotoAndMapInternal(receivedPhotoName: String) {
    suspendWithState { state ->
      val updateResult = state.swapMapAndPhoto(receivedPhotoName)

      when (updateResult) {
        is UpdateStateResult.Update -> {
          setState { copy(receivedPhotos = updateResult.update) }
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
        //TODO: remove
        receivedPhotosRepository.deleteAll()
      }

      //to avoid "Your reducer must be pure!" exceptions
      val newState = ReceivedPhotosFragmentState()
      setState { newState }

      viewModelActor.send(ActorAction.LoadReceivedPhotos(false))
    }
  }

  private suspend fun loadReceivedPhotosInternal(forced: Boolean) {
    suspendWithState { state ->
      if (state.receivedPhotosRequest is Loading) {
        return@suspendWithState
      }

      if (state.isEndReached) {
        return@suspendWithState
      }

      //to avoid "Your reducer must be pure!" exceptions
      val receivedPhotosRequest = Loading<Paged<ReceivedPhoto>>()
      setState { copy(receivedPhotosRequest = receivedPhotosRequest) }

      val firstUploadedOn = state.receivedPhotos
        .firstOrNull()
        ?.uploadedOn

      val lastUploadedOn = state.receivedPhotos
        .lastOrNull()
        ?.uploadedOn

      val request = try {
        val receivedPhotos = getReceivedPhotosUseCase.loadPageOfPhotos(
          forced,
          firstUploadedOn,
          lastUploadedOn,
          photosPerPage
        )

        intercom.tell<UploadedPhotosFragment>()
          .to(UploadedPhotosFragmentEvent.ReceivePhotosEvent.OnPhotosReceived(receivedPhotos.page.map { it }))

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

      if (newPhotos.isNotEmpty()) {
        val mapped = newPhotos.map { receivedPhoto ->
          NewReceivedPhoto(
            receivedPhoto.uploadedPhotoName,
            receivedPhoto.receivedPhotoName,
            receivedPhoto.lonLat.lon,
            receivedPhoto.lonLat.lat,
            receivedPhoto.uploadedOn
          )
        }

        intercom.tell<UploadedPhotosFragment>()
          .that(UploadedPhotosFragmentEvent.GeneralEvents.OnNewPhotosReceived(mapped))
      }

      val isEndReached = request()?.isEnd ?: false

      setState {
        copy(
          isEndReached = isEndReached,
          receivedPhotosRequest = request,
          receivedPhotos = newReceivedPhotos
        )
      }

      startReceivingService()
    }
  }

  suspend fun onReceivePhotosEvent(event: ReceivedPhotosFragmentEvent.ReceivePhotosEvent) {
    suspendWithState { state ->
      when (event) {
        is ReceivedPhotosFragmentEvent.ReceivePhotosEvent.PhotosReceived -> {

          val updateResult = state.onPhotosReceived(event.receivedPhotos)
          if (updateResult !is UpdateStateResult.Update) {
            throw IllegalStateException("Not implemented for result ${updateResult::class}")
          }

          //to avoid "Your reducer must be pure!" exceptions
          val request = Success(Paged(updateResult.update))

          setState {
            copy(
              receivedPhotosRequest = request,
              receivedPhotos = updateResult.update
            )
          }
        }
        is ReceivedPhotosFragmentEvent.ReceivePhotosEvent.OnFailed -> {
          event.error.printStackTrace()
          Timber.tag(TAG).d("Error while trying to receive photos: (${event.error.message})")
        }
      }.safe
    }
  }

  private fun startReceivingService() {
    Timber.tag(TAG).d("startReceiveingService called!")
    intercom.tell<PhotosActivity>().to(PhotosActivityEvent.StartReceivingService)
  }

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

  fun testSetState(newState: ReceivedPhotosFragmentState) {
    setState { newState }
  }

  suspend fun testGetState(): ReceivedPhotosFragmentState {
    return suspendCoroutine { continuation ->
      withState { state -> continuation.resume(state) }
    }
  }

  sealed class ActorAction {
    class LoadReceivedPhotos(val forced: Boolean) : ActorAction()
    class ResetState(val clearCache: Boolean) : ActorAction()
    class SwapPhotoAndMap(val receivedPhotoName: String) : ActorAction()
    class OnNewPhotosReceived(val newReceivedPhotos: List<NewReceivedPhoto>) : ActorAction()
    class RemovePhoto(val photoName: String) : ActorAction()
    class ReportPhoto(val photoName: String) : ActorAction()
    class FavouritePhoto(val photoName: String) : ActorAction()
    class OnPhotoReported(val photoName: String,
                          val isReported: Boolean) : ActorAction()

    class OnPhotoFavourited(val photoName: String,
                            val isFavourited: Boolean,
                            val favouritesCount: Long) : ActorAction()

    object CheckFreshPhotos : ActorAction()
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