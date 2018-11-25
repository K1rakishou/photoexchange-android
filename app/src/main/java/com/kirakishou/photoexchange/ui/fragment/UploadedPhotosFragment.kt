package com.kirakishou.photoexchange.ui.fragment

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.*
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
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
import io.reactivex.Flowable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.consumeEach
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class UploadedPhotosFragment : BaseMvRxFragment(), StateEventListener<UploadedPhotosFragmentEvent>, IntercomListener {

  @Inject
  lateinit var imageLoader: ImageLoader

  @Inject
  lateinit var viewModel: PhotosActivityViewModel

  private val TAG = "UploadedPhotosFragment"

  private val uploadedPhotoAdapterViewWidth = Constants.DEFAULT_ADAPTER_ITEM_WIDTH
  private val intervalTime = 30L

  private val photoSize by lazy { AndroidUtils.figureOutPhotosSizes(requireContext()) }
  private val columnsCount by lazy { AndroidUtils.calculateNoOfColumns(requireContext(), uploadedPhotoAdapterViewWidth) }

  override fun getFragmentLayoutId(): Int = R.layout.fragment_mvrx

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    viewModel.uploadedPhotosFragmentViewModel.photoSize = photoSize
    viewModel.uploadedPhotosFragmentViewModel.photosPerPage = columnsCount * Constants.DEFAULT_PHOTOS_PER_PAGE_COUNT

    viewModel.uploadedPhotosFragmentViewModel.selectSubscribe(this, UploadedPhotosFragmentState::takenPhotos) {
      startUploadingService()
    }

    viewModel.uploadedPhotosFragmentViewModel.selectSubscribe(this, UploadedPhotosFragmentState::uploadedPhotos) {
      startReceivingService("Starting the service after a page of uploaded photos was loaded")
    }

    launch { initRx() }
  }

  override fun onResume() {
    super.onResume()

    clearOnPauseCompositeDisposable += Flowable.interval(intervalTime, intervalTime, TimeUnit.SECONDS)
      .subscribe({ startReceivingService("Periodic check of photos to receive") })
  }

  private suspend fun initRx() {
    launch {
      viewModel.intercom.uploadedPhotosFragmentEvents.listen().consumeEach { event ->
        onStateEvent(event)
      }
    }
  }

  override fun buildEpoxyController(): AsyncEpoxyController = simpleController {
    return@simpleController withState(viewModel.uploadedPhotosFragmentViewModel) { state ->
      if (state.takenPhotos.isNotEmpty()) {
        sectionRow {
          id("queued_up_and_uploading_photos_section")
          text("Uploading photos")
        }

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

          if (state.uploadedPhotos.isEmpty()) {
            textRow {
              id("no_uploaded_photos")
              text("You have no photos yet")
            }
          } else {
            sectionRow {
              id("uploaded_photos_section")
              text("Uploaded photos")
            }

            state.uploadedPhotos.forEach { photo ->
              uploadedPhotoRow {
                id("uploaded_photo_${photo.photoId}")
                photo(photo)
              }
            }

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
      is UploadedPhotosFragmentEvent.GeneralEvents.UpdateReceiverInfo -> {
        //TODO
      }
      is UploadedPhotosFragmentEvent.GeneralEvents.OnPageSelected -> {
//          viewModel.uploadedPhotosFragmentViewModel.viewState.reset()
      }
      is UploadedPhotosFragmentEvent.GeneralEvents.PhotosReceived -> {
        viewModel.uploadedPhotosFragmentViewModel.onUpdateReceiverInfo(event.receivedPhotos)
      }
      is UploadedPhotosFragmentEvent.GeneralEvents.Invalidate -> {
        //FIXME: Hack to update the view after changing the state manually
        //(for some reason it does not getting called automatically, so I have to do it manually with this hack)
        doInvalidate()
      }
    }.safe
  }

  private fun startReceivingService(reason: String) {
    viewModel.intercom.tell<PhotosActivity>()
      .to(PhotosActivityEvent.StartReceivingService(
        PhotosActivityViewModel::class.java,
        reason)
      )
  }

  private fun startUploadingService() {
    viewModel.intercom.tell<PhotosActivity>()
      .to(PhotosActivityEvent.StartUploadingService(
        PhotosActivityViewModel::class.java,
        "There are queued up photos in the database")
      )
  }

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
