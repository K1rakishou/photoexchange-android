package com.kirakishou.photoexchange.ui.fragment


import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.Toast
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.IntercomListener
import com.kirakishou.photoexchange.helper.intercom.StateEventListener
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
import com.kirakishou.photoexchange.ui.widget.SwipeToRefreshLayout
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject

class ReceivedPhotosFragment : BaseFragment(), StateEventListener<ReceivedPhotosFragmentEvent>, IntercomListener {

    @BindView(R.id.received_photos_list)
    lateinit var receivedPhotosList: RecyclerView

    @BindView(R.id.swipe_refresh_layout)
    lateinit var swipeToRefreshLayout: SwipeToRefreshLayout

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var viewModel: PhotosActivityViewModel

    lateinit var adapter: ReceivedPhotosAdapter
    lateinit var endlessScrollListener: EndlessRecyclerOnScrollListener

    private val TAG = "ReceivedPhotosFragment"
    private val PHOTO_ADAPTER_VIEW_WIDTH = DEFAULT_ADAPTER_ITEM_WIDTH
    private val adapterClicksSubject = PublishSubject.create<ReceivedPhotosAdapter.ReceivedPhotosAdapterClickEvent>()
    private var photosPerPage = 0

    override fun getContentView(): Int = R.layout.fragment_received_photos

    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
        initRx()
        initViews()
        loadFirstPage()
    }

    override fun onFragmentViewDestroy() {
    }

    private fun initRx() {
        compositeDisposable += viewModel.receivedPhotosFragmentErrorCodeSubject
            .subscribe(this::handleKnownError)

        compositeDisposable += viewModel.intercom.receivedPhotosFragmentEvents.listen()
            .doOnNext { viewState -> onStateEvent(viewState) }
            .subscribe({ }, { Timber.tag(TAG).e(it) })

        compositeDisposable += lifecycle.getLifecycle()
            .subscribe(viewModel.receivedPhotosFragmentLifecycle::onNext)

        compositeDisposable += adapterClicksSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribe({ click -> handleAdapterClick(click) }, { Timber.tag(TAG).e(it) })
    }

    private fun initViews() {
        val columnsCount = AndroidUtils.calculateNoOfColumns(requireContext(), PHOTO_ADAPTER_VIEW_WIDTH)

        adapter = ReceivedPhotosAdapter(requireContext(), imageLoader, adapterClicksSubject)

        val layoutManager = GridLayoutManager(requireContext(), columnsCount)
        layoutManager.spanSizeLookup = ReceivedPhotosAdapterSpanSizeLookup(adapter, columnsCount)

        photosPerPage = Constants.RECEIVED_PHOTOS_PER_ROW * layoutManager.spanCount
        endlessScrollListener = EndlessRecyclerOnScrollListener(TAG, layoutManager, photosPerPage, viewModel.receivedPhotosFragmentLoadPhotosSubject)

        receivedPhotosList.layoutManager = layoutManager
        receivedPhotosList.adapter = adapter
        receivedPhotosList.clearOnScrollListeners()
        receivedPhotosList.addOnScrollListener(endlessScrollListener)

        swipeToRefreshLayout.setOnRefreshListener {
            endlessScrollListener.pauseLoading()
            viewModel.receivedPhotosFragmentRefreshPhotos.onNext(Unit)
        }
    }

    private fun loadFirstPage() {
        viewModel.receivedPhotosFragmentLoadPhotosSubject.onNext(false)
    }

    private fun handleAdapterClick(click: ReceivedPhotosAdapter.ReceivedPhotosAdapterClickEvent) {
        if (!isAdded) {
            return
        }

        when (click) {
            is ReceivedPhotosAdapter.ReceivedPhotosAdapterClickEvent.ShowFullPhoto -> {
                TODO()
            }
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
                is ReceivedPhotosFragmentEvent.GeneralEvents.ScrollToTop -> receivedPhotosList.scrollToPosition(0)
                is ReceivedPhotosFragmentEvent.GeneralEvents.ShowProgressFooter -> showProgressFooter()
                is ReceivedPhotosFragmentEvent.GeneralEvents.HideProgressFooter -> hideProgressFooter()
                is ReceivedPhotosFragmentEvent.GeneralEvents.StartRefreshing -> swipeToRefreshLayout.isRefreshing = true
                is ReceivedPhotosFragmentEvent.GeneralEvents.StopRefreshing -> swipeToRefreshLayout.isRefreshing = false
                is ReceivedPhotosFragmentEvent.GeneralEvents.AddReceivedPhotos -> addReceivedPhotosToAdapter(event.photos)
                is ReceivedPhotosFragmentEvent.GeneralEvents.ClearAdapter -> adapter.clear()
                is ReceivedPhotosFragmentEvent.GeneralEvents.ClearCache -> clearIdsCache()
            }.safe
        }
    }

    private fun clearIdsCache() {
        //TODO: remove
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
                viewModel.receivedPhotosFragmentViewState.update(newLastId = receivedPhotos.last().photoId)
                adapter.addReceivedPhotos(receivedPhotos)
            }

            if (receivedPhotos.size < photosPerPage) {
                endlessScrollListener.pauseLoading()
            }

            endlessScrollListener.pageLoaded()
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
            adapter.hideProgressFooter()
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

    private fun handleKnownError(errorCode: ErrorCode.GetReceivedPhotosErrors) {
        hideProgressFooter()

        if (!isVisible) {
            return
        }

        val message = when (errorCode) {
            is ErrorCode.GetReceivedPhotosErrors.Ok,
            is ErrorCode.GetReceivedPhotosErrors.LocalUserIdIsEmpty -> null
            is ErrorCode.GetReceivedPhotosErrors.UnknownError -> "Unknown error"
            is ErrorCode.GetReceivedPhotosErrors.DatabaseError -> "Server database error"
            is ErrorCode.GetReceivedPhotosErrors.BadRequest -> "Bad request error"
            is ErrorCode.GetReceivedPhotosErrors.NoPhotosInRequest -> "Bad request error (no photos in request)"
            is ErrorCode.GetReceivedPhotosErrors.LocalBadServerResponse -> "Bad server response error"
            is ErrorCode.GetReceivedPhotosErrors.LocalTimeout -> "Operation timeout error"
        }

        if (message != null) {
            showToast(message)
        }
    }

    private fun showToast(message: String, duration: Int = Toast.LENGTH_LONG) {
        (requireActivity() as PhotosActivity).showToast(message, duration)
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
