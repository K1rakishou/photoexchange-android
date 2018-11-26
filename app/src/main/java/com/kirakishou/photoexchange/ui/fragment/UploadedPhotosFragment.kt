package com.kirakishou.photoexchange.ui.fragment

import android.os.Bundle
import android.view.View
import com.airbnb.epoxy.AsyncEpoxyController
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.IntercomListener
import com.kirakishou.photoexchange.helper.intercom.StateEventListener
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.state.UploadedPhotosFragmentState
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.epoxy_controller.UploadedPhotosFragmentEpoxyController
import io.reactivex.Flowable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.consumeEach
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class UploadedPhotosFragment : BaseMvRxFragment(), StateEventListener<UploadedPhotosFragmentEvent>, IntercomListener {

  @Inject
  lateinit var imageLoader: ImageLoader

  @Inject
  lateinit var viewModel: PhotosActivityViewModel

  private val TAG = "UploadedPhotosFragment"

  private val controller = UploadedPhotosFragmentEpoxyController()
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

    viewModel.uploadedPhotosFragmentViewModel.subscribe(this, true) {
      doInvalidate()
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
    controller.rebuild(requireContext(), this, viewModel.uploadedPhotosFragmentViewModel)
  }

  override suspend fun onStateEvent(event: UploadedPhotosFragmentEvent) {
    when (event) {
      is UploadedPhotosFragmentEvent.GeneralEvents -> {
        kotlin.run {
          if (isAdded) {
            onUiEvent(event)
          }
        }
      }

      is UploadedPhotosFragmentEvent.PhotoUploadEvent -> {
        viewModel.uploadedPhotosFragmentViewModel.onUploadingEvent(event)
      }
    }.safe
  }

  private suspend fun onUiEvent(event: UploadedPhotosFragmentEvent.GeneralEvents) {
    when (event) {
      is UploadedPhotosFragmentEvent.GeneralEvents.OnPageSelected -> {
//          viewModel.uploadedPhotosFragmentViewModel.viewState.reset()
      }
      is UploadedPhotosFragmentEvent.GeneralEvents.UpdateReceiverInfo,
      is UploadedPhotosFragmentEvent.GeneralEvents.PhotosReceived -> {
        val receivedPhotos = when (event) {
          is UploadedPhotosFragmentEvent.GeneralEvents.UpdateReceiverInfo -> event.receivedPhotos
          is UploadedPhotosFragmentEvent.GeneralEvents.PhotosReceived -> event.receivedPhotos
          else -> throw IllegalStateException("Event not supported (${event::class})")
        }

        viewModel.uploadedPhotosFragmentViewModel.onUpdateReceiverInfo(receivedPhotos)
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
