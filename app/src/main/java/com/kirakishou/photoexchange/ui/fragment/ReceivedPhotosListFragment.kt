package com.kirakishou.photoexchange.ui.fragment


import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseFragment
import com.kirakishou.photoexchange.di.component.DaggerAllPhotoViewActivityComponent
import com.kirakishou.photoexchange.di.module.AllPhotoViewActivityModule
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mwvm.model.dto.PhotoAnswerAllFound
import com.kirakishou.photoexchange.mwvm.model.other.AdapterItem
import com.kirakishou.photoexchange.mwvm.model.other.AdapterItemType
import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer
import com.kirakishou.photoexchange.mwvm.viewmodel.AllPhotosViewActivityViewModel
import com.kirakishou.photoexchange.mwvm.viewmodel.factory.AllPhotosViewActivityViewModelFactory
import com.kirakishou.photoexchange.ui.activity.AllPhotosViewActivity
import com.kirakishou.photoexchange.ui.activity.MapActivity
import com.kirakishou.photoexchange.ui.activity.ViewPhotoFullSizeActivity
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
    private var isPhotoUploading = true

    private val loadMoreSubject = PublishSubject.create<Int>()
    private val photoAnswerClickSubject = PublishSubject.create<PhotoAnswer>()
    private val photoAnswerLongClickSubject = PublishSubject.create<PhotoAnswer>()

    override fun initViewModel(): AllPhotosViewActivityViewModel {
        return ViewModelProviders.of(activity!!, viewModelFactory).get(AllPhotosViewActivityViewModel::class.java)
    }

    override fun getContentView(): Int = R.layout.fragment_received_photos_list

    override fun initRx() {
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

        compositeDisposable += photoAnswerClickSubject
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onPhotoAnswerClick, this::onUnknownError)

        compositeDisposable += photoAnswerLongClickSubject
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onPhotoAnswerLongClick, this::onUnknownError)

        compositeDisposable += getViewModel().outputs.onScrollToTopObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ scrollToTop() }, this::onUnknownError)

        compositeDisposable += getViewModel().outputs.onShowLookingForPhotoIndicatorObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ showLookingForPhotoIndicator() }, this::onUnknownError)

        compositeDisposable += getViewModel().outputs.onShowPhotoReceivedObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onPhotoReceived, this::onUnknownError)

        compositeDisposable += getViewModel().outputs.onShowErrorWhileTryingToLookForPhotoObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ errorWhileTryingToSearchForPhoto() }, this::onUnknownError)

        compositeDisposable += getViewModel().outputs.onShowNoPhotoOnServerObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onNoPhotoOnTheServer() }, this::onUnknownError)

        compositeDisposable += getViewModel().outputs.onShowUserNeedsToUploadMorePhotosObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ userNeedsToUploadMorePhotos() }, this::onUnknownError)

        compositeDisposable += getViewModel().outputs.onStartLookingForPhotosObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ startLookingForPhotos() }, this::onUnknownError)

        compositeDisposable += getViewModel().errors.onUnknownErrorObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onUnknownError)
    }

    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
        initRecyclerView()

        getViewModel().inputs.shouldStartLookingForPhotos()
        recyclerStartLoadingItems()
    }
    
    override fun onFragmentViewDestroy() {
    }

    private fun showLookingForPhotoIndicator() {
        isPhotoUploading = false

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
        columnsCount = AndroidUtils.calculateNoOfColumns(activity!!, PHOTO_ADAPTER_VIEW_WIDTH)

        adapter = ReceivedPhotosAdapter(activity!!, photoAnswerClickSubject, photoAnswerLongClickSubject)
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

    private fun onPhotoAnswerClick(photo: PhotoAnswer) {
        val intent = Intent(activity, MapActivity::class.java)
        intent.putExtra("lon", photo.lon)
        intent.putExtra("lat", photo.lat)

        startActivity(intent)
    }

    private fun onPhotoAnswerLongClick(photo: PhotoAnswer) {
        val intent = Intent(activity, ViewPhotoFullSizeActivity::class.java)
        intent.putExtra("photo_name", photo.photoName)

        startActivity(intent)
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

    private fun scrollToTop() {
        receivedPhotosList.scrollToPosition(0)
    }

    private fun onPhotoReceived(data: PhotoAnswerAllFound) {
        Timber.d("ReceivedPhotosListFragment: onPhotoReceived()")
        check(isAdded)

        adapter.runOnAdapterHandler {
            if (data.allFound) {
                adapter.removeLookingForPhotoIndicator()
            }

            adapter.removeMessage()
            adapter.addFirst(AdapterItem(data.photoAnswer, AdapterItemType.VIEW_ITEM))

            (activity as AllPhotosViewActivity).showNewPhotoReceivedNotification()
        }
    }

    private fun onNoPhotoOnTheServer() {
        Timber.d("ReceivedPhotosListFragment: onNoPhoto()")
        check(isAdded)

        adapter.runOnAdapterHandler {
            adapter.removeLookingForPhotoIndicator()
            adapter.removeMessage()

            adapter.addLookingForPhotoIndicator()
        }
    }

    private fun errorWhileTryingToSearchForPhoto() {
        Timber.d("ReceivedPhotosListFragment: errorWhileTryingToSearchForPhoto()")
        check(isAdded)

        adapter.runOnAdapterHandler {
            adapter.removeLookingForPhotoIndicator()
            adapter.removeMessage()

            adapter.addMessage(ReceivedPhotosAdapter.MESSAGE_TYPE_ERROR)
        }
    }

    private fun userNeedsToUploadMorePhotos() {
        Timber.d("ReceivedPhotosListFragment: userNeedsToUploadMorePhotos()")
        check(isAdded)

        adapter.runOnAdapterHandler {
            adapter.removeLookingForPhotoIndicator()
            adapter.removeMessage()

            adapter.addMessage(ReceivedPhotosAdapter.MESSAGE_TYPE_UPLOAD_MORE_PHOTOS)
        }
    }

    private fun startLookingForPhotos() {
        Timber.d("ReceivedPhotosListFragment: Showing startLookingForPhotoAnswerService")
        (activity as AllPhotosViewActivity).startLookingForPhotoAnswerService()
        showLookingForPhotoIndicator()
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
        fun newInstance(): ReceivedPhotosListFragment {
            val fragment = ReceivedPhotosListFragment()
            val args = Bundle()

            fragment.arguments = args
            return fragment
        }
    }
}
