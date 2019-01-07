package com.kirakishou.photoexchange.ui.fragment


import android.os.Bundle
import android.view.View
import com.airbnb.epoxy.AsyncEpoxyController
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.di.component.fregment.ReceivedPhotosFragmentComponent
import com.kirakishou.photoexchange.di.module.fragment.ReceivedPhotosFragmentModule
import com.kirakishou.photoexchange.helper.Constants
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.IntercomListener
import com.kirakishou.photoexchange.helper.intercom.StateEventListener
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.intercom.event.ReceivedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.epoxy.controller.ReceivedPhotosFragmentEpoxyController
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ReceivedPhotosFragment : MyBaseMvRxFragment(), StateEventListener<ReceivedPhotosFragmentEvent>, IntercomListener {

  @Inject
  lateinit var viewModel: PhotosActivityViewModel

  @Inject
  lateinit var controller: ReceivedPhotosFragmentEpoxyController

  /**
   * This fragment may be attached to either PhotosActivity (and it has activity component,
   * so we can get it from the activity) or to FragmentTestingActivity which doesn't have the component.
   * In this case the test should provide that component
   * */
  private fun getFragmentComponent(): ReceivedPhotosFragmentComponent? {
    return (requireActivity() as? PhotosActivity)?.activityComponent?.plus(
      ReceivedPhotosFragmentModule()
    )
  }

  private val TAG = "ReceivedPhotosFragment"

  private val scrollSubject = PublishSubject.create<Boolean>()

  private val receivedPhotoAdapterViewWidth = Constants.DEFAULT_ADAPTER_ITEM_WIDTH
  private val throttleTime = 200L

  private val photoSize by lazy { AndroidUtils.figureOutPhotosSizes(requireContext()) }
  private val columnsCount by lazy { AndroidUtils.calculateNoOfColumns(requireContext(), receivedPhotoAdapterViewWidth) }

  override fun getFragmentLayoutId(): Int = R.layout.fragment_mvrx

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initRx()

    viewModel.receivedPhotosFragmentViewModel.photoSize = photoSize
    viewModel.receivedPhotosFragmentViewModel.photosPerPage = columnsCount * Constants.DEFAULT_PHOTOS_PER_PAGE_COUNT

    viewModel.receivedPhotosFragmentViewModel.subscribe(this, true) {
      doInvalidate()
    }

    swipeRefreshLayout.setOnRefreshListener {
      swipeRefreshLayout.isRefreshing = false
      viewModel.receivedPhotosFragmentViewModel.resetState(true)
    }

    viewModel.receivedPhotosFragmentViewModel.loadReceivedPhotos(false)
  }

  private fun initRx() {
    compositeDisposable += scrollSubject
      .subscribeOn(Schedulers.io())
      .distinctUntilChanged()
      .throttleFirst(throttleTime, TimeUnit.MILLISECONDS)
      .subscribe({ isScrollingDown ->
        viewModel.intercom.tell<PhotosActivity>()
          .that(PhotosActivityEvent.ScrollEvent(isScrollingDown))
      })

    compositeDisposable += viewModel.intercom.receivedPhotosFragmentEvents.listen()
      .subscribe({ event ->
        launch { onStateEvent(event) }
      })
  }

  override fun buildEpoxyController(): AsyncEpoxyController = simpleController {
    controller.rebuild(
      requireContext(),
      this@ReceivedPhotosFragment,
      this,
      viewModel.receivedPhotosFragmentViewModel
    )
  }

  override suspend fun onStateEvent(event: ReceivedPhotosFragmentEvent) {
    when (event) {
      is ReceivedPhotosFragmentEvent.GeneralEvents -> {
        onUiEvent(event)
      }
      is ReceivedPhotosFragmentEvent.ReceivePhotosEvent -> {
        viewModel.receivedPhotosFragmentViewModel.onReceivePhotosEvent(event)
      }
    }.safe
  }

  private fun onUiEvent(event: ReceivedPhotosFragmentEvent.GeneralEvents) {
    if (!isAdded) {
      return
    }

    when (event) {
      is ReceivedPhotosFragmentEvent.GeneralEvents.ScrollToTop -> {
        recyclerView.scrollToPosition(0)
      }
      is ReceivedPhotosFragmentEvent.GeneralEvents.OnNewPhotosReceived -> {
        Timber.tag(TAG).d("OnNewPhotosReceived, currentState = ${lifecycle.getCurrentState()}")
        viewModel.receivedPhotosFragmentViewModel.onNewPhotosReceived(event.newReceivedPhotos)
      }
      is ReceivedPhotosFragmentEvent.GeneralEvents.RemovePhoto -> {
        viewModel.receivedPhotosFragmentViewModel.removePhoto(event.photoName)
      }
      is ReceivedPhotosFragmentEvent.GeneralEvents.PhotoReported -> {
        viewModel.receivedPhotosFragmentViewModel.onPhotoReported(
          event.photoName,
          event.isReported
        )
      }
      is ReceivedPhotosFragmentEvent.GeneralEvents.PhotoFavourited -> {
        viewModel.receivedPhotosFragmentViewModel.onPhotoFavourited(
          event.photoName,
          event.isFavourited,
          event.favouritesCount
        )
      }
    }.safe
  }

  override fun resolveDaggerDependency() {
    getFragmentComponent()?.inject(this)
  }

  companion object {
    fun newInstance(): ReceivedPhotosFragment {
      val fragment = ReceivedPhotosFragment()
      val args = Bundle()

      fragment.arguments = args
      return fragment
    }
  }
}
