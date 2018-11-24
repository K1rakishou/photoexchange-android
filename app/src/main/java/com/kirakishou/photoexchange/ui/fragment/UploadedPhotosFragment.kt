package com.kirakishou.photoexchange.ui.fragment

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.*
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.PhotoSize
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.IntercomListener
import com.kirakishou.photoexchange.helper.intercom.StateEventListener
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.model.photo.UploadingPhoto
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.state.UploadedPhotosFragmentState
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.adapter.epoxy.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.consumeEach
import timber.log.Timber
import javax.inject.Inject


class UploadedPhotosFragment : BaseMvRxFragment(), StateEventListener<UploadedPhotosFragmentEvent>, IntercomListener {

  @Inject
  lateinit var imageLoader: ImageLoader

  @Inject
  lateinit var viewModel: PhotosActivityViewModel

  private val TAG = "UploadedPhotosFragment"

  private val photoSize by lazy {
    val density = requireContext().resources.displayMetrics.density

    return@lazy if (density < 2.0) {
      PhotoSize.Small
    } else if (density >= 2.0 && density < 3.0) {
      PhotoSize.Medium
    } else {
      PhotoSize.Big
    }
  }

  private val uploadedPhotoAdapterViewWidth = Constants.DEFAULT_ADAPTER_ITEM_WIDTH
  private val columnsCount by lazy {
    AndroidUtils.calculateNoOfColumns(requireContext(), uploadedPhotoAdapterViewWidth)
  }

  override fun getFragmentLayoutId(): Int = R.layout.fragment_mvrx

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    viewModel.uploadedPhotosFragmentViewModel.photoSize = photoSize
    viewModel.uploadedPhotosFragmentViewModel.photosPerPage = columnsCount * Constants.DEFAULT_PHOTOS_PER_PAGE_COUNT

    viewModel.uploadedPhotosFragmentViewModel.selectSubscribe(this, UploadedPhotosFragmentState::takenPhotos) {
      viewModel.intercom.tell<PhotosActivity>()
        .to(PhotosActivityEvent.StartUploadingService(PhotosActivityViewModel::class.java,
          "There are queued up photos in the database"))
    }

    viewModel.uploadedPhotosFragmentViewModel.selectSubscribe(this, UploadedPhotosFragmentState::uploadedPhotos) {
      viewModel.intercom.tell<PhotosActivity>()
        .to(PhotosActivityEvent.StartReceivingService(PhotosActivityViewModel::class.java,
          "Starting the service after a page of uploaded photos was loaded"))
    }

    launch { initRx() }
  }

  private suspend fun initRx() {
//    compositeDisposable += viewModel.uploadedPhotosFragmentViewModel.knownErrors
//      .subscribe({ errorCode -> handleKnownErrors(errorCode) })
//
//    compositeDisposable += viewModel.uploadedPhotosFragmentViewModel.unknownErrors
//      .subscribe({ error -> handleUnknownErrors(error) })
//
//    compositeDisposable += failedToUploadPhotoButtonClicksSubject
//      .observeOn(AndroidSchedulers.mainThread())
//      .subscribe({
//        viewModel.intercom.tell<PhotosActivity>()
//          .that(PhotosActivityEvent.FailedToUploadPhotoButtonClicked(it))
//      }, { Timber.tag(TAG).e(it) })
//
//    compositeDisposable += loadMoreSubject
//      .subscribe({ viewModel.uploadedPhotosFragmentViewModel.loadMorePhotos() })
//
//    compositeDisposable += scrollSubject
//      .subscribeOn(Schedulers.io())
//      .distinctUntilChanged()
//      .throttleFirst(200, TimeUnit.MILLISECONDS)
//      .subscribe({ isScrollingDown ->
//        viewModel.intercom.tell<PhotosActivity>()
//          .that(PhotosActivityEvent.ScrollEvent(isScrollingDown))
//      })
//
    launch {
      viewModel.intercom.uploadedPhotosFragmentEvents.listen().consumeEach { event ->
        onStateEvent(event)
      }
    }
  }

  override fun buildEpoxyController(): AsyncEpoxyController = simpleController {
    return@simpleController withState(viewModel.uploadedPhotosFragmentViewModel) { state ->
      if (state.takenPhotos.isNotEmpty()) {
        state.takenPhotos.forEach { photo ->
          when (photo.photoState) {
            PhotoState.PHOTO_TAKEN -> {
            }
            PhotoState.PHOTO_QUEUED_UP -> {
              queuedUpPhotoRow {
                id("queued_up_photo_${photo.id}")
                photo(photo)
                callback { _ -> viewModel.uploadedPhotosFragmentViewModel.cancelPhotoUploading(photo.id) }
              }
            }
            PhotoState.PHOTO_UPLOADING -> {
              val uploadingPhoto = photo as UploadingPhoto

              uploadingPhotoRow {
                id("uploading_photo_${photo.id}")
                photo(uploadingPhoto)
                progress(uploadingPhoto.progress)
              }
            }
          }
        }
      }

      when (state.uploadedPhotosRequest) {
        is Loading -> {
          Timber.tag(TAG).d("Loading uploaded photos")

          loadingRow {
            id("uploaded_photos_loading_row")
          }
        }
        is Success -> {
          Timber.tag(TAG).d("Success uploaded photos")

          state.uploadedPhotos.forEach { photo ->
            uploadedPhotoRow {
              id("uploaded_photo_${photo.photoId}")
              photo(photo)
            }
          }

          if (state.uploadedPhotos.isEmpty()) {
            textRow {
              id("no_uploaded_photos")
              text("You have no photos yet")
            }
          } else {
            if (state.isEndReached) {
              textRow {
                id("list_end_footer_text")
                text("End of the list reached.\nClick here to reload")
                callback { _ ->
                  Timber.tag(TAG).d("Reloading")
                  viewModel.uploadedPhotosFragmentViewModel.resetState()
                }
              }
            } else {
              loadingRow {
                //we should change the id to trigger the binding
                id("load_next_page_${state.uploadedPhotos.size}")
                onBind { _, _, _ -> viewModel.uploadedPhotosFragmentViewModel.loadUploadedPhotos() }
              }
            }
          }
        }
        is Fail -> {
          Timber.tag(TAG).d("Fail uploaded photos")

          textRow {
            val exceptionMessage = state.uploadedPhotosRequest.error.message ?: "Unknown error message"
            Toast.makeText(requireContext(), "Exception message is: \"$exceptionMessage\"", Toast.LENGTH_LONG).show()

            id("unknown_error")
            text("Unknown error has occurred while trying to load photos from the database. \nClick here to retry")
            callback { _ ->
              Timber.tag(TAG).d("Reloading")
              viewModel.uploadedPhotosFragmentViewModel.resetState()
            }
          }
        }
        is Uninitialized -> {
          //do nothing
        }
      }.safe
    }
  }

  override suspend fun onStateEvent(event: UploadedPhotosFragmentEvent) {
    if (!isAdded) {
      return
    }

    when (event) {
      is UploadedPhotosFragmentEvent.GeneralEvents -> {
        onUiEvent(event)
      }

      is UploadedPhotosFragmentEvent.PhotoUploadEvent -> {
        viewModel.uploadedPhotosFragmentViewModel.onUploadingEvent(event)
      }
    }.safe
  }

  private suspend fun onUiEvent(event: UploadedPhotosFragmentEvent.GeneralEvents) {
    if (!isAdded) {
      return
    }

    when (event) {
      is UploadedPhotosFragmentEvent.GeneralEvents.ScrollToTop -> {
//          uploadedPhotosList.scrollToPosition(0)
      }
      is UploadedPhotosFragmentEvent.GeneralEvents.RemovePhoto -> {
//          adapter.removePhotoById(event.photo.id)
      }
      is UploadedPhotosFragmentEvent.GeneralEvents.AddPhoto -> {
//          adapter.addTakenPhoto(event.photo)
      }
      is UploadedPhotosFragmentEvent.GeneralEvents.PhotoRemoved -> {
//          if (adapter.getQueuedUpAndFailedPhotosCount() == 0) {
//            triggerPhotosLoading()
//          } else {
//            //do nothing
//          }
      }
      is UploadedPhotosFragmentEvent.GeneralEvents.AfterPermissionRequest -> {
//          triggerPhotosLoading()
      }
      is UploadedPhotosFragmentEvent.GeneralEvents.UpdateReceiverInfo -> {
//          event.receivedPhotos.forEach {
//            adapter.updateUploadedPhotoSetReceiverInfo(it.uploadedPhotoName)
//          }
      }
      is UploadedPhotosFragmentEvent.GeneralEvents.OnPageSelected -> {
//          viewModel.uploadedPhotosFragmentViewModel.viewState.reset()
      }
      is UploadedPhotosFragmentEvent.GeneralEvents.ShowTakenPhotos -> {
//          addTakenPhotosToAdapter(event.takenPhotos)
      }
      is UploadedPhotosFragmentEvent.GeneralEvents.ShowUploadedPhotos -> {
//          addUploadedPhotosToAdapter(event.uploadedPhotos)
      }
      is UploadedPhotosFragmentEvent.GeneralEvents.PhotoReceived -> {
        //TODO
//          adapter.updateUploadedPhotoSetReceiverInfo(event.takenPhotoName)
      }
      is UploadedPhotosFragmentEvent.GeneralEvents.Invalidate -> {
        //FIXME: Hack to update the view after changing the state manually
        //(for some reason it does not getting called automatically, so I have to do it manually with this hack)
        doInvalidate()
      }
    }.safe
  }

//  private fun addUploadedPhotosToAdapter(uploadedPhotos: List<UploadedPhoto>) {
//    if (!isAdded) {
//      return
//    }
//
//    uploadedPhotosList.post {
//      if (uploadedPhotos.isNotEmpty()) {
//        adapter.addUploadedPhotos(uploadedPhotos)
//      }
//
//      endlessScrollListener.pageLoaded()
//
//      if (adapter.getUploadedPhotosCount() == 0) {
//        adapter.showMessageFooter("You have no uploaded photos")
//        return@post
//      }
//
//      if (uploadedPhotos.size < photosPerPage) {
//        adapter.showMessageFooter("End of the list reached")
//        endlessScrollListener.reachedEnd()
//      }
//    }
//  }

//  private fun addTakenPhotosToAdapter(takenPhotos: List<TakenPhoto>) {
//    if (!isAdded) {
//      return
//    }
//
//    uploadedPhotosList.post {
//      if (takenPhotos.isNotEmpty()) {
//        adapter.clear()
//        adapter.addTakenPhotos(takenPhotos)
//      } else {
//        adapter.showMessageFooter("You have no taken photos")
//      }
//    }
//  }
//
//  private fun showProgressFooter() {
//    if (!isAdded) {
//      return
//    }
//
//    uploadedPhotosList.post {
//      adapter.showProgressFooter()
//    }
//  }
//
//  private fun hideProgressFooter() {
//    if (!isAdded) {
//      return
//    }
//
//    uploadedPhotosList.post {
//      adapter.clearFooter()
//    }
//  }

//  private fun handleKnownErrors(errorCode: ErrorCode) {
//    //TODO: do we even need this method?
//    hideProgressFooter()
//    adapter.updateAllPhotosState(PhotoState.FAILED_TO_UPLOAD)
//    (activity as? PhotosActivity)?.showKnownErrorMessage(errorCode)
//  }
//
//  private fun handleUnknownErrors(error: Throwable) {
//    adapter.updateAllPhotosState(PhotoState.FAILED_TO_UPLOAD)
//    (activity as? PhotosActivity)?.showUnknownErrorMessage(error)
//
//    Timber.tag(TAG).e(error)
//  }

  override fun resolveDaggerDependency() {
    (requireActivity() as PhotosActivity).activityComponent
      .inject(this)
  }

  companion object {
    fun newInstance(): UploadedPhotosFragment {
      val fragment = UploadedPhotosFragment()
      val args = Bundle()

      fragment.arguments = args
      return fragment
    }
  }
}
