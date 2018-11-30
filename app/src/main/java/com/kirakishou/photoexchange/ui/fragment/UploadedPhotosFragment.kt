package com.kirakishou.photoexchange.ui.fragment

import android.os.Bundle
import android.view.View
import com.airbnb.epoxy.AsyncEpoxyController
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.IntercomListener
import com.kirakishou.photoexchange.helper.intercom.StateEventListener
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.helper.Constants
import com.kirakishou.photoexchange.helper.RxLifecycle
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.epoxy.controller.UploadedPhotosFragmentEpoxyController
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.zipWith
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


class UploadedPhotosFragment : BaseMvRxFragment(), StateEventListener<UploadedPhotosFragmentEvent>, IntercomListener {

  @Inject
  lateinit var viewModel: PhotosActivityViewModel

  private val TAG = "UploadedPhotosFragment"

  private val controller = UploadedPhotosFragmentEpoxyController()
  private val uploadedPhotoAdapterViewWidth = Constants.DEFAULT_ADAPTER_ITEM_WIDTH

  private val photoSize by lazy { AndroidUtils.figureOutPhotosSizes(requireContext()) }
  private val columnsCount by lazy { AndroidUtils.calculateNoOfColumns(requireContext(), uploadedPhotoAdapterViewWidth) }

  override fun getFragmentLayoutId(): Int = R.layout.fragment_mvrx

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    viewModel.uploadedPhotosFragmentViewModel.photoSize = photoSize
    viewModel.uploadedPhotosFragmentViewModel.photosPerPage = columnsCount * Constants.DEFAULT_PHOTOS_PER_PAGE_COUNT

    viewModel.uploadedPhotosFragmentViewModel.subscribe(this, true) {
      doInvalidate()
    }

    swipeRefreshLayout.setOnRefreshListener {
      swipeRefreshLayout.isRefreshing = false
      viewModel.uploadedPhotosFragmentViewModel.resetState()
    }

    initRx()
  }

  private fun initRx() {
    compositeDisposable += viewModel.intercom.uploadedPhotosFragmentEvents.listen()
      .zipWith(lifecycle.getLifecycleObservable())
      .filter { (_, lifecycle) -> lifecycle.isAtLeast(RxLifecycle.FragmentState.Created) }
      .map { (event, _) -> event }
      .subscribe(
        { event -> launch { onStateEvent(event) } },
        { error -> Timber.tag(TAG).e(error) }
      )
  }

  override fun buildEpoxyController(): AsyncEpoxyController = simpleController {
    controller.rebuild(requireContext(), this, viewModel.uploadedPhotosFragmentViewModel)
  }

  override suspend fun onStateEvent(event: UploadedPhotosFragmentEvent) {
    when (event) {
      is UploadedPhotosFragmentEvent.GeneralEvents -> {
        onUiEvent(event)
      }

      is UploadedPhotosFragmentEvent.PhotoUploadEvent -> {
        viewModel.uploadedPhotosFragmentViewModel.onUploadingEvent(event)
      }
      is UploadedPhotosFragmentEvent.ReceivePhotosEvent -> {
        viewModel.uploadedPhotosFragmentViewModel.onReceiveEvent(event)
      }
    }.safe
  }

  private suspend fun onUiEvent(event: UploadedPhotosFragmentEvent.GeneralEvents) {
    if (!isAdded) {
      return
    }

    when (event) {
      UploadedPhotosFragmentEvent.GeneralEvents.FetchFreshPhotos -> {
        Timber.tag(TAG).d("FetchFreshPhotos received, lifecycle = ${lifecycle.getCurrentLifecycle()}")
        viewModel.uploadedPhotosFragmentViewModel.fetchFreshPhotos()
      }
    }.safe
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
