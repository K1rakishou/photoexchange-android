package com.kirakishou.photoexchange.ui.fragment


import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.Toast
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.RxLifecycle
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
import com.kirakishou.photoexchange.ui.viewstate.ReceivedPhotosFragmentViewState
import com.kirakishou.photoexchange.ui.widget.EndlessRecyclerOnScrollListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
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

    private val loadMoreSubject = PublishSubject.create<Int>()
    private val scrollSubject = PublishSubject.create<Boolean>()
    private val adapterClicksSubject = PublishSubject.create<ReceivedPhotosAdapter.ReceivedPhotosAdapterClickEvent>()

    private val viewState = ReceivedPhotosFragmentViewState()
    private var photosPerPage = 0

    override fun getContentView(): Int = R.layout.fragment_received_photos

    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
        initRx()
        initRecyclerView()
        loadFirstPage()
    }

    override fun onFragmentViewDestroy() {
    }

    private fun initRx() {
        compositeDisposable += viewModel.intercom.receivedPhotosFragmentEvents.listen()
            .doOnNext { viewState -> onStateEvent(viewState) }
            .subscribe({ }, { Timber.tag(TAG).e(it) })

        compositeDisposable += Observables.combineLatest(loadMoreSubject, lifecycle.getLifecycle())
            .concatMap { (nextPage, lifecycle) ->
                return@concatMap Observable.just(lifecycle)
                    .filter { _lifecycle -> _lifecycle.isAtLeast(RxLifecycle.FragmentState.Resumed) }
                    .map { nextPage }
            }
            .doOnNext { endlessScrollListener.pageLoading() }
            .concatMap { viewModel.loadNextPageOfReceivedPhotos(viewState.lastId, photosPerPage) }
            .subscribe({ result ->
                when (result) {
                    is Either.Value -> addReceivedPhotosToAdapter(result.value)
                    is Either.Error -> handleError(result.error)
                }
            }, { Timber.tag(TAG).e(it) })

        compositeDisposable += adapterClicksSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribe({ click -> handleAdapterClick(click) }, { Timber.tag(TAG).e(it) })

        compositeDisposable += scrollSubject
            .distinctUntilChanged()
            .subscribeOn(AndroidSchedulers.mainThread())
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
        endlessScrollListener = EndlessRecyclerOnScrollListener(TAG, layoutManager, 2, loadMoreSubject, scrollSubject, 1)

        receivedPhotosList.layoutManager = layoutManager
        receivedPhotosList.adapter = adapter
        receivedPhotosList.clearOnScrollListeners()
        receivedPhotosList.addOnScrollListener(endlessScrollListener)
    }

    private fun loadFirstPage() {
        loadMoreSubject.onNext(0)
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
                is ReceivedPhotosFragmentEvent.GeneralEvents.OnTabClicked -> {
                    //TODO
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
                viewState.updateLastId(receivedPhotos.last().photoId)
                adapter.addReceivedPhotos(receivedPhotos)
            }

            if (adapter.itemCount == 0) {
                adapter.showMessageFooter("You have not received any photos yet")
                endlessScrollListener.pageLoaded()
                return@post
            }

            if (receivedPhotos.size < photosPerPage) {
                adapter.showMessageFooter("Bottom of the list reached")
                endlessScrollListener.reachedEnd()
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

    private fun handleError(errorCode: ErrorCode.GetReceivedPhotosErrors) {
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
