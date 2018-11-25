package com.kirakishou.photoexchange.ui.fragment


import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.withState
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.IntercomListener
import com.kirakishou.photoexchange.helper.intercom.StateEventListener
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.intercom.event.ReceivedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.adapter.ReceivedPhotosAdapter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.consumeEach
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ReceivedPhotosFragment : BaseMvRxFragment(), StateEventListener<ReceivedPhotosFragmentEvent>, IntercomListener {

  @BindView(R.id.received_photos_list)
  lateinit var receivedPhotosList: RecyclerView

  @Inject
  lateinit var imageLoader: ImageLoader

  @Inject
  lateinit var viewModel: PhotosActivityViewModel

  private val TAG = "ReceivedPhotosFragment"
  private val scrollSubject = PublishSubject.create<Boolean>()
  private val adapterClicksSubject = PublishSubject.create<ReceivedPhotosAdapter.ReceivedPhotosAdapterClickEvent>()

  private val receivedPhotoAdapterViewWidth = Constants.DEFAULT_ADAPTER_ITEM_WIDTH
  private val columnsCount by lazy { AndroidUtils.calculateNoOfColumns(requireContext(), receivedPhotoAdapterViewWidth) }

  override fun getFragmentLayoutId(): Int = R.layout.fragment_received_photos

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    viewModel.receivedPhotosFragmentViewModel.photosPerPage = columnsCount * Constants.DEFAULT_PHOTOS_PER_PAGE_COUNT

    viewModel.receivedPhotosFragmentViewModel.subscribe(this, true) {
      doInvalidate()
    }

    launch { initRx() }
  }

  private suspend fun initRx() {
    compositeDisposable += adapterClicksSubject
      .subscribeOn(AndroidSchedulers.mainThread())
      .subscribe({ click -> handleAdapterClick(click) }, { Timber.tag(TAG).e(it) })

    compositeDisposable += scrollSubject
      .subscribeOn(Schedulers.io())
      .distinctUntilChanged()
      .throttleFirst(200, TimeUnit.MILLISECONDS)
      .subscribe({ isScrollingDown ->
        viewModel.intercom.tell<PhotosActivity>()
          .that(PhotosActivityEvent.ScrollEvent(isScrollingDown))
      })

    launch {
      viewModel.intercom.receivedPhotosFragmentEvents.listen().consumeEach { event ->
        onStateEvent(event)
      }
    }
  }

  override fun buildEpoxyController(): AsyncEpoxyController = simpleController {
    return@simpleController withState(viewModel.receivedPhotosFragmentViewModel) { state ->

    }
  }

  private fun handleAdapterClick(click: ReceivedPhotosAdapter.ReceivedPhotosAdapterClickEvent) {
    if (!isAdded) {
      return
    }

    when (click) {
      is ReceivedPhotosAdapter.ReceivedPhotosAdapterClickEvent.SwitchShowMapOrPhoto -> {
        switchShowMapOrPhoto(click.photoName)
      }
    }.safe
  }

  override suspend fun onStateEvent(event: ReceivedPhotosFragmentEvent) {

    when (event) {
      is ReceivedPhotosFragmentEvent.GeneralEvents -> {
        kotlin.run {
          if (isAdded) {
            onUiEvent(event)
          }
        }
      }
      is ReceivedPhotosFragmentEvent.ReceivePhotosEvent -> {
        //TODO: move to viewModel
        onReceivePhotosEvent(event)
      }
    }.safe
  }

  private suspend fun onUiEvent(event: ReceivedPhotosFragmentEvent.GeneralEvents) {
    when (event) {
      is ReceivedPhotosFragmentEvent.GeneralEvents.ScrollToTop -> {
        recyclerView.scrollToPosition(0)
      }
      is ReceivedPhotosFragmentEvent.GeneralEvents.OnPageSelected -> {
      }
    }.safe
  }

  private fun onReceivePhotosEvent(event: ReceivedPhotosFragmentEvent.ReceivePhotosEvent) {
    when (event) {
      is ReceivedPhotosFragmentEvent.ReceivePhotosEvent.PhotosReceived -> {
        //TODO:
//          adapter.addReceivedPhoto(event.receivedPhoto)
      }
      is ReceivedPhotosFragmentEvent.ReceivePhotosEvent.OnFailed -> {
        //TODO: do nothing here???
      }
    }.safe
  }

  private fun switchShowMapOrPhoto(photoName: String) {
    if (!isAdded) {
      return
    }

    receivedPhotosList.post {
      //TODO
//      adapter.switchShowMapOrPhoto(photoName)
    }
  }

  override fun resolveDaggerDependency() {
    (requireActivity() as PhotosActivity).activityComponent
      .inject(this)
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
