package com.kirakishou.photoexchange.ui.fragment

import android.os.Bundle
import android.view.View
import com.airbnb.epoxy.AsyncEpoxyController
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.di.component.fregment.UploadedPhotosFragmentComponent
import com.kirakishou.photoexchange.di.module.fragment.UploadedPhotosFragmentModule
import com.kirakishou.photoexchange.helper.Constants
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.IntercomListener
import com.kirakishou.photoexchange.helper.intercom.StateEventListener
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.epoxy.controller.UploadedPhotosFragmentEpoxyController
import io.reactivex.rxkotlin.plusAssign
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


class UploadedPhotosFragment : MyBaseMvRxFragment(), StateEventListener<UploadedPhotosFragmentEvent>, IntercomListener {

  @Inject
  lateinit var viewModel: PhotosActivityViewModel

  @Inject
  lateinit var controller: UploadedPhotosFragmentEpoxyController

  /**
   * This fragment may be attached to either PhotosActivity (and it has activity component,
   * so we can get it from the activity) or to FragmentTestingActivity which doesn't have the component.
   * In this case the test should provide that component
   * */
  private fun getFragmentComponent(): UploadedPhotosFragmentComponent? {
    return (requireActivity() as? PhotosActivity)?.activityComponent?.plus(UploadedPhotosFragmentModule())
  }

  private val TAG = "UploadedPhotosFragment"
  private val photoSize by lazy { AndroidUtils.figureOutPhotosSizes(requireContext()) }

  override fun getFragmentLayoutId(): Int = R.layout.fragment_mvrx

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initRx()

    viewModel.uploadedPhotosFragmentViewModel.photoSize = photoSize
    viewModel.uploadedPhotosFragmentViewModel.photosPerPage = spanCount * Constants.DEFAULT_PHOTOS_PER_PAGE_COUNT

    // Probably it would be a better idea to subscribe here not to the whole state but to
    // just some properties that are responsible for building epoxy models.
    // In the current situation all of the state properties are responsible for that, but once
    // there are more properties in the state it should become more reasonable.
    viewModel.uploadedPhotosFragmentViewModel.subscribe(this, true) {
      doInvalidate()
    }

    swipeRefreshLayout.setOnRefreshListener {
      swipeRefreshLayout.isRefreshing = false
      viewModel.uploadedPhotosFragmentViewModel.resetState(true)
    }

    viewModel.uploadedPhotosFragmentViewModel.loadQueuedUpPhotos()
  }

  private fun initRx() {
    compositeDisposable += viewModel.intercom.uploadedPhotosFragmentEvents.listen()
      .subscribe(
        { event -> launch { onStateEvent(event) } },
        { error -> Timber.tag(TAG).e(error) }
      )
  }

  override fun onResume() {
    super.onResume()

    viewModel.uploadedPhotosFragmentViewModel.checkFreshPhotos()
  }

  override fun buildEpoxyController(): AsyncEpoxyController = simpleController {
    controller.rebuild(
      requireContext(),
      this@UploadedPhotosFragment,
      this,
      viewModel.uploadedPhotosFragmentViewModel
    )
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
      is UploadedPhotosFragmentEvent.GeneralEvents.ScrollToTop -> {
        recyclerView.scrollToPosition(0)
      }
      is UploadedPhotosFragmentEvent.GeneralEvents.OnNewPhotosReceived -> {
        viewModel.uploadedPhotosFragmentViewModel.onNewPhotosReceived(event.newReceivedPhotos)
      }
    }.safe
  }

  override fun resolveDaggerDependency() {
    getFragmentComponent()?.inject(this)
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
