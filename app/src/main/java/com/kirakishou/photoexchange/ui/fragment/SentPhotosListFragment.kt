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
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvvm.model.*
import com.kirakishou.photoexchange.mvvm.viewmodel.AllPhotosViewActivityViewModel
import com.kirakishou.photoexchange.mvvm.viewmodel.factory.AllPhotosViewActivityViewModelFactory
import com.kirakishou.photoexchange.ui.activity.AllPhotosViewActivity
import com.kirakishou.photoexchange.ui.adapter.TakenPhotosAdapter
import com.kirakishou.photoexchange.ui.widget.EndlessRecyclerOnScrollListener
import com.kirakishou.photoexchange.ui.widget.TakenPhotosAdapterSpanSizeLookup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject

class SentPhotosListFragment : BaseFragment<AllPhotosViewActivityViewModel>() {

    @BindView(R.id.sent_photos_list)
    lateinit var sentPhotosRv: RecyclerView

    @Inject
    lateinit var viewModelFactory: AllPhotosViewActivityViewModelFactory

    private lateinit var adapter: TakenPhotosAdapter
    private lateinit var endlessScrollListener: EndlessRecyclerOnScrollListener

    private val PHOTO_ADAPTER_VIEW_WIDTH = 288
    private val PHOTOS_PER_PAGE = 5
    private var columnsCount: Int = 1
    private val retryButtonSubject = PublishSubject.create<UploadedPhoto>()
    private val loadMoreSubject = PublishSubject.create<Int>()

    override fun initViewModel(): AllPhotosViewActivityViewModel {
        return ViewModelProviders.of(activity, viewModelFactory).get(AllPhotosViewActivityViewModel::class.java)
    }

    override fun getContentView(): Int = R.layout.fragment_sent_photos_list

    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
        initRx()
        initRecyclerView()

        val isUploadingPhoto = arguments.getBoolean("is_uploading_photo", false)
        if (!isUploadingPhoto) {
            recyclerStartLoadingItems()
        }
    }

    override fun onFragmentViewDestroy() {
        PhotoExchangeApplication.refWatcher.watch(this, this::class.simpleName)
    }

    private fun recyclerStartLoadingItems() {
        //FIXME:
        //HACK
        //For some mysterious reason if we do not add a delay before calling addProgressFooter
        //loadMoreSubject won't get any observables at all, so we have to add slight delay
        //I have no idea why is this happening

        adapter.runOnAdapterHandlerWithDelay(500) {
            adapter.addProgressFooter()
        }
    }

    private fun initRecyclerView() {
        columnsCount = AndroidUtils.calculateNoOfColumns(activity, PHOTO_ADAPTER_VIEW_WIDTH)

        val noPhotosUploadedMessage = context.getString(R.string.no_photos_uploaded)

        adapter = TakenPhotosAdapter(activity, retryButtonSubject, noPhotosUploadedMessage)
        adapter.init()

        val layoutManager = GridLayoutManager(activity, columnsCount)
        layoutManager.spanSizeLookup = TakenPhotosAdapterSpanSizeLookup(adapter, columnsCount)

        endlessScrollListener = EndlessRecyclerOnScrollListener(layoutManager, loadMoreSubject)

        sentPhotosRv.layoutManager = layoutManager
        sentPhotosRv.clearOnScrollListeners()
        sentPhotosRv.addOnScrollListener(endlessScrollListener)
        sentPhotosRv.adapter = adapter
        sentPhotosRv.setHasFixedSize(true)
    }

    private fun initRx() {
        compositeDisposable += loadMoreSubject
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { Timber.d("page: $it") }
                .doOnNext { adapter.addProgressFooter() }
                .doOnNext(this::fetchPage)
                .observeOn(Schedulers.io())
                .zipWith(getViewModel().outputs.onPageReceivedObservable())
                .map { it.second }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { adapter.removeProgressFooter() }
                .doOnNext {
                    Timber.e("items count: ${it.size}")
                }
                .subscribe(this::onPageReceived, this::onUnknownError)

        compositeDisposable += retryButtonSubject
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onRetryButtonClicked, this::onUnknownError)
    }

    private fun fetchPage(page: Int) {
        getViewModel().inputs.fetchOnePage(page, PHOTOS_PER_PAGE * columnsCount)
    }

    private fun onPageReceived(uploadedPhotosList: List<UploadedPhoto>) {
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

    private fun onRetryButtonClicked(uploadedPhoto: UploadedPhoto) {
        Timber.e("photoName: ${uploadedPhoto.photoName}")
    }

    fun onPhotoUploaded(photo: UploadedPhoto) {
        Timber.d("onPhotoUploaded()")

        check(isAdded)
        //adapter.removeProgressFooter()

        /*adapter.runOnAdapterHandler {
            adapter.addFirst(AdapterItem(photo, AdapterItemType.VIEW_ITEM))
        }*/

        recyclerStartLoadingItems()
    }

    fun onFailedToUploadPhoto() {
        Timber.d("onFailedToUploadPhoto()")

        check(isAdded)
        //adapter.removeProgressFooter()

        adapter.runOnAdapterHandler {
            adapter.addFirst(AdapterItem(AdapterItemType.VIEW_FAILED_TO_UPLOAD))
        }

        recyclerStartLoadingItems()
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
        fun newInstance(isUploadingPhoto: Boolean): SentPhotosListFragment {
            val fragment = SentPhotosListFragment()
            val args = Bundle()
            args.putBoolean("is_uploading_photo", isUploadingPhoto)

            fragment.arguments = args
            return fragment
        }
    }
}
