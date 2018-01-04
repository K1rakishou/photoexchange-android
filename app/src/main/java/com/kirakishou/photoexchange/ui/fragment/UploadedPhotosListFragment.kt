package com.kirakishou.photoexchange.ui.fragment


import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseFragment
import com.kirakishou.photoexchange.di.component.DaggerAllPhotoViewActivityComponent
import com.kirakishou.photoexchange.di.module.AllPhotoViewActivityModule
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.extension.filterMulticastEvent
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mwvm.model.other.AdapterItem
import com.kirakishou.photoexchange.mwvm.model.other.AdapterItemType
import com.kirakishou.photoexchange.mwvm.model.other.Constants.PHOTO_ADAPTER_VIEW_WIDTH
import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto
import com.kirakishou.photoexchange.mwvm.model.state.PhotoUploadingState
import com.kirakishou.photoexchange.mwvm.viewmodel.AllPhotosViewActivityViewModel
import com.kirakishou.photoexchange.mwvm.viewmodel.factory.AllPhotosViewActivityViewModelFactory
import com.kirakishou.photoexchange.ui.activity.AllPhotosViewActivity
import com.kirakishou.photoexchange.ui.adapter.UploadedPhotosAdapter
import com.kirakishou.photoexchange.ui.widget.EndlessRecyclerOnScrollListener
import com.kirakishou.photoexchange.ui.widget.UploadedPhotosAdapterSpanSizeLookup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject

class UploadedPhotosListFragment : BaseFragment<AllPhotosViewActivityViewModel>() {

    @BindView(R.id.sent_photos_list)
    lateinit var sentPhotosRv: RecyclerView

    @Inject
    lateinit var viewModelFactory: AllPhotosViewActivityViewModelFactory

    @Inject
    lateinit var imageLoader: ImageLoader

    private lateinit var adapter: UploadedPhotosAdapter
    private lateinit var endlessScrollListener: EndlessRecyclerOnScrollListener
    private lateinit var layoutManager: GridLayoutManager

    private val ttag = "[${this::class.java.simpleName}]: "
    private val DELAY_BEFORE_PROGRESS_FOOTER_ADDED = 100L
    private val PHOTOS_PER_PAGE = 5
    private var columnsCount: Int = 1

    private val loadMoreSubject = PublishSubject.create<Int>()
    private var isPhotoUploading = false

    override fun initViewModel(): AllPhotosViewActivityViewModel {
        return ViewModelProviders.of(activity!!, viewModelFactory).get(AllPhotosViewActivityViewModel::class.java)
    }

    override fun getContentView(): Int = R.layout.fragment_uploaded_photos_list

    override fun initRx() {
        compositeDisposable += loadMoreSubject
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { adapter.addProgressFooter() }
                .doOnNext(this::fetchPage)
                .observeOn(Schedulers.io())
                .zipWith(getViewModel().outputs.onUploadedPhotosPageReceivedObservable())
                .map { it.second }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { adapter.removeProgressFooter() }
                .subscribe(this::onPageReceived, this::onUnknownError)

        compositeDisposable += getViewModel().outputs.onPhotoUploadingStateObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .filterMulticastEvent(UploadedPhotosListFragment::class.java)
                .subscribe(this::onPhotoUploadingState, this::onUnknownError)

        compositeDisposable += getViewModel().errors.onUnknownErrorObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onUnknownError)
    }

    private fun onPhotoUploadingState(state: PhotoUploadingState) {
        when (state) {
            is PhotoUploadingState.StartPhotoUploading -> {
                Timber.tag(ttag).d("onPhotoUploadingState() PhotoUploadingState.StartPhotoUploading")

                adapter.runOnAdapterHandler {
                    adapter.removeMessageFooter()
                    adapter.addPhotoUploadingIndicator()
                }
            }

            is PhotoUploadingState.PhotoUploaded -> {
                Timber.tag(ttag).d("onPhotoUploadingState() PhotoUploadingState.PhotoUploaded")

                adapter.runOnAdapterHandler {
                    adapter.add(AdapterItem(state.photo!!, AdapterItemType.VIEW_ITEM))
                }
            }

            is PhotoUploadingState.AllPhotosUploaded -> {
                Timber.tag(ttag).d("onPhotoUploadingState() PhotoUploadingState.AllPhotosUploaded")

                adapter.runOnAdapterHandler {
                    adapter.removePhotoUploadingIndicator()
                }
            }

            is PhotoUploadingState.FailedToUploadPhoto -> {
                Timber.tag(ttag).d("onPhotoUploadingState() PhotoUploadingState.FailedToUploadPhoto")

                adapter.runOnAdapterHandler {
                    adapter.removePhotoUploadingIndicator()
                    adapter.addMessageFooter()
                    //adapter.addFirst(AdapterItem(AdapterItemType.VIEW_FAILED_TO_UPLOAD))
                }
            }

            else -> IllegalStateException("Bad value")
        }
    }

    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
        initRecyclerView()
        recyclerStartLoadingItems()
    }

    override fun onFragmentViewDestroy() {
    }

    override fun onResume() {
        getViewModel().inputs.beginReceivingEvents(this::class.java)
        getViewModel().inputs.startLookingForPhotos()
        getViewModel().inputs.startPhotosUploading()
        super.onResume()
    }

    override fun onPause() {
        getViewModel().inputs.stopReceivingEvents(this::class.java)
        super.onPause()
    }

    private fun recyclerStartLoadingItems() {
        //FIXME:
        //HACK
        //For some mysterious reason if we do not add a delay before calling addProgressFooter
        //loadMoreSubject won't get any observables from EndlessRecyclerOnScrollListener at all, so we have to add a slight delay.
        //The subscription to loadMoreSubject happens before scroll listener generates any observables,
        //so I have no idea why loadMoreSubject doesn't receive any observables

        adapter.runOnAdapterHandlerWithDelay(DELAY_BEFORE_PROGRESS_FOOTER_ADDED) {
            adapter.addProgressFooter()
        }
    }

    private fun initRecyclerView() {
        columnsCount = AndroidUtils.calculateNoOfColumns(activity!!, PHOTO_ADAPTER_VIEW_WIDTH)

        adapter = UploadedPhotosAdapter(activity!!, imageLoader)
        adapter.init()

        layoutManager = GridLayoutManager(activity, columnsCount)
        layoutManager.spanSizeLookup = UploadedPhotosAdapterSpanSizeLookup(adapter, columnsCount)

        endlessScrollListener = EndlessRecyclerOnScrollListener(layoutManager, loadMoreSubject)

        sentPhotosRv.layoutManager = layoutManager
        sentPhotosRv.clearOnScrollListeners()
        sentPhotosRv.addOnScrollListener(endlessScrollListener)
        sentPhotosRv.adapter = adapter
        sentPhotosRv.setHasFixedSize(true)
    }

    private fun fetchPage(page: Int) {
        val count = PHOTOS_PER_PAGE * columnsCount
        getViewModel().inputs.fetchOnePageUploadedPhotos(page * count, count)
    }

    private fun onPageReceived(uploadedPhotosList: List<TakenPhoto>) {
        adapter.runOnAdapterHandler {
            endlessScrollListener.pageLoaded()

            if (uploadedPhotosList.size < PHOTOS_PER_PAGE * columnsCount) {
                endlessScrollListener.reachedEnd()
            }

            if (uploadedPhotosList.isNotEmpty()) {
                for (photo in uploadedPhotosList) {
                    adapter.add(AdapterItem(photo, AdapterItemType.VIEW_ITEM))
                }
            } else {
                if (adapter.itemCount == 0) {
                    adapter.addMessageFooter()
                }
            }
        }
    }

    private fun onUnknownError(error: Throwable) {
        (activity as AllPhotosViewActivity).onUnknownError(error)
    }

    override fun resolveDaggerDependency() {
        DaggerAllPhotoViewActivityComponent.builder()
                .applicationComponent(PhotoExchangeApplication.applicationComponent)
                .allPhotoViewActivityModule(AllPhotoViewActivityModule(activity as AllPhotosViewActivity))
                .build()
                .inject(this)
    }

    companion object {
        fun newInstance(): UploadedPhotosListFragment {
            val fragment = UploadedPhotosListFragment()
            val args = Bundle()

            fragment.arguments = args
            return fragment
        }
    }
}
