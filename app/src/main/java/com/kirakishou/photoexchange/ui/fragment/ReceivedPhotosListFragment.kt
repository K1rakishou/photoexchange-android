package com.kirakishou.photoexchange.ui.fragment


import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseFragment
import com.kirakishou.photoexchange.di.component.DaggerAllPhotoViewActivityComponent
import com.kirakishou.photoexchange.di.module.AllPhotoViewActivityModule
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mwvm.model.other.AdapterItem
import com.kirakishou.photoexchange.mwvm.model.other.AdapterItemType
import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer
import com.kirakishou.photoexchange.mwvm.viewmodel.AllPhotosViewActivityViewModel
import com.kirakishou.photoexchange.mwvm.viewmodel.factory.AllPhotosViewActivityViewModelFactory
import com.kirakishou.photoexchange.ui.activity.AllPhotosViewActivity
import com.kirakishou.photoexchange.ui.adapter.ReceivedPhotosAdapter
import com.kirakishou.photoexchange.ui.widget.EndlessRecyclerOnScrollListener
import com.kirakishou.photoexchange.ui.widget.ReceivedPhotosAdapterSpanSizeLookup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject

class ReceivedPhotosListFragment : BaseFragment<AllPhotosViewActivityViewModel>() {

    @BindView(R.id.received_photos_list)
    lateinit var receivedPhotosList: RecyclerView

    @Inject
    lateinit var viewModelFactory: AllPhotosViewActivityViewModelFactory

    private lateinit var adapter: ReceivedPhotosAdapter
    private lateinit var endlessScrollListener: EndlessRecyclerOnScrollListener
    private lateinit var layoutManager: GridLayoutManager

    private val DELAY_BEFORE_PROGRESS_FOOTER_ADDED = 100L
    private val PHOTO_ADAPTER_VIEW_WIDTH = 288
    private val PHOTOS_PER_PAGE = 5
    private var columnsCount: Int = 1

    private val loadMoreSubject = PublishSubject.create<Int>()

    override fun initViewModel(): AllPhotosViewActivityViewModel {
        return ViewModelProviders.of(activity, viewModelFactory).get(AllPhotosViewActivityViewModel::class.java)
    }

    override fun getContentView(): Int = R.layout.fragment_received_photos_list

    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
        initRx()
        initRecyclerView()

        val isPhotoUploading = arguments.getBoolean("is_photo_uploading", false)
        if (!isPhotoUploading) {
            (activity as AllPhotosViewActivity).startLookingForPhotoAnswerService()
            addLookingForPhotoIndicator()
        }

        recyclerStartLoadingItems()
    }

    override fun onFragmentViewDestroy() {
        PhotoExchangeApplication.refWatcher.watch(this, this::class.simpleName)
    }

    private fun initRx() {
        compositeDisposable += loadMoreSubject
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { adapter.addProgressFooter() }
                .doOnNext(this::fetchPage)
                .observeOn(Schedulers.io())
                .zipWith(getViewModel().outputs.onReceivedPhotosPageReceivedObservable())
                .map { it.second }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { adapter.removeProgressFooter() }
                .subscribe(this::onPageReceived, this::onUnknownError)
    }

    fun addLookingForPhotoIndicator() {
        adapter.runOnAdapterHandlerWithDelay(DELAY_BEFORE_PROGRESS_FOOTER_ADDED) {
            adapter.addLookingForPhotoIndicator()
        }
    }

    private fun recyclerStartLoadingItems() {
        //FIXME:
        //HACK
        //For some mysterious reason if we do not add a delay before calling addProgressFooter
        //loadMoreSubject won't get any observables from EndlessRecyclerOnScrollListener at all, so we have to add a slight delay
        //The subscription to loadMoreSubject happens before scroll listener generates any observables,
        //so I have no idea why loadMoreSubject doesn't receive any observables

        adapter.runOnAdapterHandlerWithDelay(DELAY_BEFORE_PROGRESS_FOOTER_ADDED) {
            adapter.addProgressFooter()
        }
    }

    private fun initRecyclerView() {
        columnsCount = AndroidUtils.calculateNoOfColumns(activity, PHOTO_ADAPTER_VIEW_WIDTH)

        adapter = ReceivedPhotosAdapter(activity)
        adapter.init()

        layoutManager = GridLayoutManager(activity, columnsCount)
        layoutManager.spanSizeLookup = ReceivedPhotosAdapterSpanSizeLookup(adapter, columnsCount)

        endlessScrollListener = EndlessRecyclerOnScrollListener(layoutManager, loadMoreSubject)

        receivedPhotosList.layoutManager = layoutManager
        receivedPhotosList.clearOnScrollListeners()
        receivedPhotosList.addOnScrollListener(endlessScrollListener)
        receivedPhotosList.adapter = adapter
        receivedPhotosList.setHasFixedSize(true)
    }

    private fun fetchPage(page: Int) {
        val count = PHOTOS_PER_PAGE * columnsCount
        getViewModel().inputs.fetchOnePageReceivedPhotos(page * count, count)
    }

    private fun onPageReceived(photoAnswerList: List<PhotoAnswer>) {
        adapter.runOnAdapterHandler {
            endlessScrollListener.pageLoaded()

            if (photoAnswerList.size < PHOTOS_PER_PAGE * columnsCount) {
                endlessScrollListener.reachedEnd()
            }

            if (photoAnswerList.isNotEmpty()) {
                for (photo in photoAnswerList) {
                    adapter.add(AdapterItem(photo, AdapterItemType.VIEW_ITEM))
                }
            } else {
                if (adapter.itemCount == 0) {
                    //adapter.addMessageFooter()
                }
            }
        }
    }

    fun scrollToTop() {
        check(isAdded)

        receivedPhotosList.scrollToPosition(0)
    }

    fun onPhotoReceived(photo: PhotoAnswer) {
        Timber.d("onPhotoReceived()")
        check(isAdded)

        adapter.runOnAdapterHandler {
            adapter.removeLookingForPhotoIndicator()
            adapter.addFirst(AdapterItem(photo, AdapterItemType.VIEW_ITEM))

            (activity as AllPhotosViewActivity).showNewPhotoReceivedNotification()
        }
    }

    fun onNoPhoto() {
        Timber.d("onNoPhoto()")
        check(isAdded)
    }

    fun errorWhileTryingToSearchForPhoto() {
        Timber.d("errorWhileTryingToSearchForPhoto()")
        check(isAdded)

        adapter.runOnAdapterHandler {
            adapter.removeLookingForPhotoIndicator()
        }
    }

    fun userNeedsToUploadMorePhotos() {
        Timber.d("userNeedsToUploadMorePhotos()")
        check(isAdded)

        adapter.runOnAdapterHandler {
            adapter.removeLookingForPhotoIndicator()
        }
    }

    private fun onUnknownError(error: Throwable) {
        Timber.e(error)
    }

    override fun resolveDaggerDependency() {
        DaggerAllPhotoViewActivityComponent.builder()
                .applicationComponent(PhotoExchangeApplication.applicationComponent)
                .allPhotoViewActivityModule(AllPhotoViewActivityModule(activity as AllPhotosViewActivity))
                .build()
                .inject(this)
    }

    companion object {
        fun newInstance(isPhotoUploading: Boolean): ReceivedPhotosListFragment {
            val fragment = ReceivedPhotosListFragment()
            val args = Bundle()
            args.putBoolean("is_photo_uploading", isPhotoUploading)

            fragment.arguments = args
            return fragment
        }
    }
}
