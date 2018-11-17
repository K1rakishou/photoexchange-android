package com.kirakishou.photoexchange.ui.fragment


import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.IntercomListener
import com.kirakishou.photoexchange.helper.intercom.StateEventListener
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.intercom.event.ReceivedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.model.other.Constants.DEFAULT_ADAPTER_ITEM_WIDTH
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.adapter.ReceivedPhotosAdapter
import com.kirakishou.photoexchange.ui.adapter.ReceivedPhotosAdapterSpanSizeLookup
import com.kirakishou.photoexchange.ui.widget.EndlessRecyclerOnScrollListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ReceivedPhotosFragment : BaseFragment(), StateEventListener<ReceivedPhotosFragmentEvent>, IntercomListener {

  @BindView(R.id.received_photos_list)
  lateinit var receivedPhotosList: RecyclerView

  @Inject
  lateinit var imageLoader: ImageLoader

  @Inject
  lateinit var viewModel: PhotosActivityViewModel

  lateinit var adapter: ReceivedPhotosAdapter
  lateinit var endlessScrollListener: EndlessRecyclerOnScrollListener

  private val TAG = "ReceivedPhotosFragment"
  private val PHOTO_ADAPTER_VIEW_WIDTH = DEFAULT_ADAPTER_ITEM_WIDTH

  private val loadMoreSubject = PublishSubject.create<Unit>()
  private val scrollSubject = PublishSubject.create<Boolean>()
  private val adapterClicksSubject = PublishSubject.create<ReceivedPhotosAdapter.ReceivedPhotosAdapterClickEvent>()

  private var photosPerPage = 0

  override fun getContentView(): Int = R.layout.fragment_received_photos

  override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
    viewModel.receivedPhotosFragmentViewModel.viewState.reset()

    initRx()
    initRecyclerView()
  }

  override fun onFragmentViewDestroy() {
  }

  private fun initRx() {
    compositeDisposable += viewModel.intercom.receivedPhotosFragmentEvents.listen()
      .subscribe({ viewState -> onStateEvent(viewState) }, { Timber.tag(TAG).e(it) })

    compositeDisposable += viewModel.receivedPhotosFragmentViewModel.knownErrors
      .subscribe({ errorCode -> handleKnownErrors(errorCode) })

    compositeDisposable += viewModel.receivedPhotosFragmentViewModel.unknownErrors
      .subscribe({ error -> handleUnknownErrors(error) })

    compositeDisposable += loadMoreSubject
      .subscribe({ viewModel.receivedPhotosFragmentViewModel.loadMorePhotos() })

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
  }

  private fun initRecyclerView() {
    val columnsCount = AndroidUtils.calculateNoOfColumns(requireContext(), PHOTO_ADAPTER_VIEW_WIDTH)

    adapter = ReceivedPhotosAdapter(requireContext(), imageLoader, adapterClicksSubject)

    val layoutManager = GridLayoutManager(requireContext(), columnsCount)
    layoutManager.spanSizeLookup = ReceivedPhotosAdapterSpanSizeLookup(adapter, columnsCount)

    photosPerPage = Constants.RECEIVED_PHOTOS_PER_ROW * layoutManager.spanCount
    //TODO: visible threshold should be less than photosPerPage count
    endlessScrollListener = EndlessRecyclerOnScrollListener(TAG, layoutManager, 2, loadMoreSubject, scrollSubject)

    receivedPhotosList.layoutManager = layoutManager
    receivedPhotosList.adapter = adapter
    receivedPhotosList.clearOnScrollListeners()
    receivedPhotosList.addOnScrollListener(endlessScrollListener)
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

  override fun onStateEvent(event: ReceivedPhotosFragmentEvent) {
    if (!isAdded) {
      return
    }

    requireActivity().runOnUiThread {
      when (event) {
        is ReceivedPhotosFragmentEvent.GeneralEvents -> {
          onUiEvent(event)
        }
        is ReceivedPhotosFragmentEvent.ReceivePhotosEvent -> {
          onReceivePhotosEvent(event)
        }
      }.safe
    }
  }

  private fun onUiEvent(event: ReceivedPhotosFragmentEvent.GeneralEvents) {
    if (!isAdded) {
      return
    }

    receivedPhotosList.post {
      when (event) {
        is ReceivedPhotosFragmentEvent.GeneralEvents.ScrollToTop -> {
          receivedPhotosList.scrollToPosition(0)
        }
        is ReceivedPhotosFragmentEvent.GeneralEvents.ShowProgressFooter -> {
          showProgressFooter()
        }
        is ReceivedPhotosFragmentEvent.GeneralEvents.HideProgressFooter -> {
          hideProgressFooter()
        }
        is ReceivedPhotosFragmentEvent.GeneralEvents.OnPageSelected -> {
          viewModel.receivedPhotosFragmentViewModel.viewState.reset()
        }
        is ReceivedPhotosFragmentEvent.GeneralEvents.PageIsLoading -> {
        }
        is ReceivedPhotosFragmentEvent.GeneralEvents.ShowReceivedPhotos -> {
          addReceivedPhotosToAdapter(event.photos)
        }
      }.safe
    }
  }

  private fun onReceivePhotosEvent(event: ReceivedPhotosFragmentEvent.ReceivePhotosEvent) {
    if (!isAdded) {
      return
    }

    receivedPhotosList.post {
      when (event) {
        is ReceivedPhotosFragmentEvent.ReceivePhotosEvent.PhotoReceived -> {
          adapter.addReceivedPhoto(event.receivedPhoto)
        }
        is ReceivedPhotosFragmentEvent.ReceivePhotosEvent.OnFailed,
        is ReceivedPhotosFragmentEvent.ReceivePhotosEvent.OnUnknownError -> {
          //TODO: do nothing here???
        }
      }.safe
    }
  }

  private fun addReceivedPhotosToAdapter(receivedPhotos: List<ReceivedPhoto>) {
    if (!isAdded) {
      return
    }

    receivedPhotosList.post {
      if (receivedPhotos.isNotEmpty()) {
        viewModel.receivedPhotosFragmentViewModel.viewState.updateLastId(receivedPhotos.last().photoId)
        adapter.addReceivedPhotos(receivedPhotos)
      }

      endlessScrollListener.pageLoaded()

      if (adapter.itemCount == 0) {
        adapter.showMessageFooter("You have not received any photos yet")
        return@post
      }

      if (receivedPhotos.size < photosPerPage) {
        adapter.showMessageFooter("End of the list reached")
        endlessScrollListener.reachedEnd()
      }
    }
  }

  private fun showProgressFooter() {
    if (!isAdded) {
      return
    }

    receivedPhotosList.post {
      adapter.showProgressFooter()
    }
  }

  private fun hideProgressFooter() {
    if (!isAdded) {
      return
    }

    receivedPhotosList.post {
      adapter.clearFooter()
    }
  }

  private fun switchShowMapOrPhoto(photoName: String) {
    if (!isAdded) {
      return
    }

    receivedPhotosList.post {
      adapter.switchShowMapOrPhoto(photoName)
    }
  }

  private fun handleKnownErrors(errorCode: ErrorCode) {
    when (errorCode) {
      is ErrorCode.GetReceivedPhotosErrors -> {
        hideProgressFooter()
      }
    }

    (activity as? PhotosActivity)?.showKnownErrorMessage(errorCode)
  }

  private fun handleUnknownErrors(error: Throwable) {
    (activity as? PhotosActivity)?.showUnknownErrorMessage(error)
    Timber.tag(TAG).e(error)
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
