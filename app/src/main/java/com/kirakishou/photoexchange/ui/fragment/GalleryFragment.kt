package com.kirakishou.photoexchange.ui.fragment


import android.os.Bundle
import android.view.View
import com.airbnb.epoxy.AsyncEpoxyController
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.di.module.fragment.GalleryFragmentModule
import com.kirakishou.photoexchange.helper.Constants
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.IntercomListener
import com.kirakishou.photoexchange.helper.intercom.StateEventListener
import com.kirakishou.photoexchange.helper.intercom.event.GalleryFragmentEvent
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.epoxy.controller.GalleryFragmentEpoxyController
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class GalleryFragment : BaseMvRxFragment(), StateEventListener<GalleryFragmentEvent>, IntercomListener {

  @Inject
  lateinit var viewModel: PhotosActivityViewModel

  @Inject
  lateinit var controller: GalleryFragmentEpoxyController

  private val fragmentComponent by lazy {
    (requireActivity() as PhotosActivity).activityComponent
      .plus(GalleryFragmentModule())
  }

  private val TAG = "GalleryFragment"
  private val galleryPhotoAdapterViewWidth = Constants.DEFAULT_ADAPTER_ITEM_WIDTH
  private val recyclerViewScrollEventsThrottleTimeMs = 200L

  private val photoSize by lazy { AndroidUtils.figureOutPhotosSizes(requireContext()) }
  private val columnsCount by lazy { AndroidUtils.calculateNoOfColumns(requireContext(), galleryPhotoAdapterViewWidth) }

  private val scrollSubject = PublishSubject.create<Boolean>()

  override fun getFragmentLayoutId(): Int = R.layout.fragment_mvrx

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initRx()

    viewModel.galleryFragmentViewModel.photoSize = photoSize
    viewModel.galleryFragmentViewModel.photosPerPage = columnsCount * Constants.DEFAULT_PHOTOS_PER_PAGE_COUNT

    viewModel.galleryFragmentViewModel.subscribe(this, true) {
      doInvalidate()
    }

    swipeRefreshLayout.setOnRefreshListener {
      swipeRefreshLayout.isRefreshing = false
      viewModel.galleryFragmentViewModel.resetState(true)
    }

    viewModel.galleryFragmentViewModel.loadGalleryPhotos(false)
  }

  override fun onDestroyView() {
    super.onDestroyView()

    controller.destroy()
  }

  private fun initRx() {
    compositeDisposable += scrollSubject
      .subscribeOn(Schedulers.io())
      .distinctUntilChanged()
      .throttleFirst(recyclerViewScrollEventsThrottleTimeMs, TimeUnit.MILLISECONDS)
      .subscribe({ isScrollingDown ->
        viewModel.intercom.tell<PhotosActivity>()
          .that(PhotosActivityEvent.ScrollEvent(isScrollingDown))
      })

    compositeDisposable += viewModel.intercom.galleryFragmentEvents.listen()
      .subscribe({ event ->
        launch { onStateEvent(event) }
      })
  }

  override fun buildEpoxyController(): AsyncEpoxyController = simpleController {
    controller.rebuild(
      requireContext(),
      this@GalleryFragment,
      this,
      viewModel.galleryFragmentViewModel
    )
  }

  override suspend fun onStateEvent(event: GalleryFragmentEvent) {
    if (!isAdded) {
      return
    }

    when (event) {
      GalleryFragmentEvent.GeneralEvents.ScrollToTop -> {
        recyclerView.scrollToPosition(0)
      }
      is GalleryFragmentEvent.GeneralEvents.RemovePhoto -> {
        viewModel.galleryFragmentViewModel.removePhoto(event.photoName)
      }
    }.safe
  }

  override fun resolveDaggerDependency() {
    fragmentComponent.inject(this)
  }

  companion object {
    fun newInstance(): GalleryFragment {
      val fragment = GalleryFragment()
      val args = Bundle()

      fragment.arguments = args
      return fragment
    }
  }
}
