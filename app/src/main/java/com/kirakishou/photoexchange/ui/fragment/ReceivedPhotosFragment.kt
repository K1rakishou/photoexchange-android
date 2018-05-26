package com.kirakishou.photoexchange.ui.fragment


import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.extension.filterErrorCodes
import com.kirakishou.photoexchange.helper.intercom.StateEventListener
import com.kirakishou.photoexchange.helper.intercom.event.BaseEvent
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.ReceivePhotosEvent
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.adapter.ReceivedPhotosAdapter
import com.kirakishou.photoexchange.ui.adapter.ReceivedPhotosAdapterSpanSizeLookup
import com.kirakishou.photoexchange.helper.intercom.event.ReceivedPhotosFragmentEvent
import com.kirakishou.photoexchange.ui.widget.EndlessRecyclerOnScrollListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject

class ReceivedPhotosFragment : BaseFragment(), StateEventListener {

    @BindView(R.id.received_photos_list)
    lateinit var receivedPhotosList: RecyclerView

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var viewModel: PhotosActivityViewModel

    lateinit var adapter: ReceivedPhotosAdapter
    lateinit var endlessScrollListener: EndlessRecyclerOnScrollListener

    private val TAG = "ReceivedPhotosFragment"
    private val PHOTO_ADAPTER_VIEW_WIDTH = 288
    private val loadMoreSubject = PublishSubject.create<Int>()
    private var photosPerPage = 0
    private var lastId = Long.MAX_VALUE

    override fun getContentView(): Int = R.layout.fragment_received_photos

    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
        initRx()
        initRecyclerView()

        loadPhotos()
    }

    override fun onFragmentViewDestroy() {
    }

    private fun initRx() {
        compositeDisposable += viewModel.errorCodesSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .filterErrorCodes(ReceivedPhotosFragment::class.java)
            .filter { isVisible }
            .doOnNext { handleError(it) }
            .subscribe()

//        compositeDisposable += viewModel.onPhotoFindEventSubject
//            .subscribeOn(AndroidSchedulers.mainThread())
//            .observeOn(AndroidSchedulers.mainThread())
//            .doOnNext { event -> onReceivePhotosEvent(event) }
//            .doOnError { Timber.tag(TAG).e(it) }
//            .subscribe()

        compositeDisposable += viewModel.eventForwarder.getReceivedPhotosFragmentEventsStream()
            .observeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { viewState -> onStateEvent(viewState) }
            .doOnError { Timber.tag(TAG).e(it) }
            .subscribe()
    }

    private fun loadPhotos() {
        compositeDisposable += viewModel.loadNextPageOfReceivedPhotos(lastId, photosPerPage)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { photos -> addReceivedPhotosToAdapter(photos) }
            .doOnError { Timber.tag(TAG).e(it) }
            .subscribe()
    }

    private fun initRecyclerView() {
        val columnsCount = AndroidUtils.calculateNoOfColumns(requireContext(), PHOTO_ADAPTER_VIEW_WIDTH)

        adapter = ReceivedPhotosAdapter(requireContext(), imageLoader)
        adapter.init()

        val layoutManager = GridLayoutManager(requireContext(), columnsCount)
        layoutManager.spanSizeLookup = ReceivedPhotosAdapterSpanSizeLookup(adapter, columnsCount)
        photosPerPage = Constants.RECEIVED_PHOTOS_PER_ROW * layoutManager.spanCount

        endlessScrollListener = EndlessRecyclerOnScrollListener(layoutManager, photosPerPage, loadMoreSubject)

        receivedPhotosList.layoutManager = layoutManager
        receivedPhotosList.adapter = adapter
        receivedPhotosList.clearOnScrollListeners()
        receivedPhotosList.addOnScrollListener(endlessScrollListener)
    }

    override fun onStateEvent(event: BaseEvent) {
        if (!isAdded) {
            return
        }

        requireActivity().runOnUiThread {
            when (event) {
                is ReceivedPhotosFragmentEvent -> {
                    onFragmentEvent(event)
                }
                is ReceivePhotosEvent -> {
                    onReceivePhotosEvent(event)
                }
            }
        }
    }

    private fun onFragmentEvent(event: ReceivedPhotosFragmentEvent) {
        when (event) {
            is ReceivedPhotosFragmentEvent.ScrollToTop -> {
                receivedPhotosList.scrollToPosition(0)
            }
        }
    }

    private fun onReceivePhotosEvent(event: ReceivePhotosEvent) {
        if (!isAdded) {
            return
        }

        requireActivity().runOnUiThread {
            when (event) {
                is ReceivePhotosEvent.OnPhotoReceived -> {
                    adapter.addPhotoAnswer(event.receivedPhoto)
                }
                is ReceivePhotosEvent.OnFailed,
                is ReceivePhotosEvent.OnUnknownError -> {
                    //do nothing here
                }
            }
        }
    }

    private fun addReceivedPhotosToAdapter(receivedPhotos: List<ReceivedPhoto>) {
        if (!isAdded) {
            return
        }

        requireActivity().runOnUiThread {
            if (receivedPhotos.isNotEmpty()) {
                adapter.addPhotoAnswers(receivedPhotos)
            } else {
                //TODO: show notification that no photos has been uploaded yet
            }
        }
    }

    private fun handleError(errorCode: ErrorCode) {
        (requireActivity() as PhotosActivity).showErrorCodeToast(errorCode)
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
