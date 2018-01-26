package com.kirakishou.photoexchange.ui.fragment


import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.Toast
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseFragment
import com.kirakishou.photoexchange.di.component.DaggerAllPhotoViewActivityComponent
import com.kirakishou.photoexchange.di.module.AllPhotoViewActivityModule
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.extension.filterMulticastEvent
import com.kirakishou.photoexchange.helper.service.FindPhotoAnswerService
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mwvm.model.adapter.AdapterItem
import com.kirakishou.photoexchange.mwvm.model.adapter.AdapterItemType
import com.kirakishou.photoexchange.mwvm.model.other.Constants.PHOTO_ADAPTER_VIEW_WIDTH
import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer
import com.kirakishou.photoexchange.mwvm.model.state.LookingForPhotoState
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
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject

class ReceivedPhotosListFragment : BaseFragment<AllPhotosViewActivityViewModel>() {

    @BindView(R.id.received_photos_list)
    lateinit var receivedPhotosList: RecyclerView

    @Inject
    lateinit var viewModelFactory: AllPhotosViewActivityViewModelFactory

    @Inject
    lateinit var imageLoader: ImageLoader

    private lateinit var adapter: ReceivedPhotosAdapter
    private lateinit var endlessScrollListener: EndlessRecyclerOnScrollListener
    private lateinit var layoutManager: GridLayoutManager

    private val ttag = "[${this::class.java.simpleName}]: "
    private val DELAY_BEFORE_PROGRESS_FOOTER_ADDED = 100L
    private val PHOTOS_PER_PAGE = 5
    private var columnsCount: Int = 1
    private val totalItemsOnPageCount = PHOTOS_PER_PAGE * columnsCount

    private val loadMoreSubject = PublishSubject.create<Int>()
    private val photoClickSubject = PublishSubject.create<ReceivedPhotosAdapter.PhotoAnswerClick>()

    override fun initViewModel(): AllPhotosViewActivityViewModel {
        return ViewModelProviders.of(activity!!, viewModelFactory).get(AllPhotosViewActivityViewModel::class.java)
    }

    override fun getContentView(): Int = R.layout.fragment_received_photos_list

    override fun initRx() {
        compositeDisposable += loadMoreSubject
                .subscribeOn(AndroidSchedulers.mainThread())
                .doOnNext { adapterAddProgressFooter() }
                .doOnNext { page -> getViewModel().inputs.fetchOnePageReceivedPhotos(page * totalItemsOnPageCount, totalItemsOnPageCount) }
                .doOnError(this::onUnknownError)
                .subscribe()

        compositeDisposable += getViewModel().outputs.onReceivedPhotosPageReceivedObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .doOnNext { adapterRemoveProgressFooter() }
                .doOnNext(this::onPageReceived)
                .doOnError(this::onUnknownError)
                .subscribe()

        compositeDisposable += photoClickSubject
                .subscribeOn(AndroidSchedulers.mainThread())
                .doOnError(this::onUnknownError)
                .subscribe(this::onPhotoAnswerClick)

        compositeDisposable += getViewModel().outputs.onScrollToTopObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .doOnError(this::onUnknownError)
                .subscribe({ scrollToTop() })

        compositeDisposable += getViewModel().outputs.onShowLookingForPhotoIndicatorObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .doOnError(this::onUnknownError)
                .subscribe({ showLookingForPhotoIndicator() })

        compositeDisposable += getViewModel().outputs.onLookingForPhotoStateObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .filterMulticastEvent(ReceivedPhotosListFragment::class.java)
                .doOnError(this::onUnknownError)
                .subscribe(this::onLookingForPhotoState)

        compositeDisposable += getViewModel().outputs.onStartLookingForPhotosObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .doOnError(this::onUnknownError)
                .subscribe({ startLookingForPhotos() })

        compositeDisposable += getViewModel().outputs.onShowUploadMorePhotosMessageObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .doOnError(this::onUnknownError)
                .subscribe({ onUploadMorePhotos() })

        compositeDisposable += getViewModel().outputs.onDeletePhotoAnswerFromDatabaseObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .doOnError(this::onUnknownError)
                .subscribe(this::onDeletePhotoAnswerFromDatabase)

        compositeDisposable += getViewModel().outputs.onDeletePhotoConfirmedObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .doOnError(this::onUnknownError)
                .subscribe(this::onDeletePhotoConfirmed)

        compositeDisposable += getViewModel().errors.onUnknownErrorObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .doOnError(this::onUnknownError)
                .subscribe(this::onUnknownError)
    }

    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
        initRecyclerView()
        recyclerStartLoadingItems()
    }
    
    override fun onFragmentViewDestroy() {
    }

    override fun onResume() {
        super.onResume()

        getViewModel().inputs.beginReceivingEvents(this::class.java)
        getViewModel().inputs.startLookingForPhotos()

        showIndicatorIfServiceIsRunning()
    }

    override fun onPause() {
        super.onPause()

        getViewModel().inputs.stopReceivingEvents(this::class.java)
    }

    private fun adapterRemoveProgressFooter() {
        adapter.runOnAdapterHandler {
            adapter.removeProgressFooter()
        }
    }

    private fun adapterAddProgressFooter() {
        adapter.runOnAdapterHandler {
            adapter.addProgressFooter()
        }
    }

    private fun showIndicatorIfServiceIsRunning() {
        if (FindPhotoAnswerService.isAlreadyRunning(context!!)) {
            showLookingForPhotoIndicator()
        }
    }

    private fun showLookingForPhotoIndicator() {
        adapter.runOnAdapterHandlerWithDelay(DELAY_BEFORE_PROGRESS_FOOTER_ADDED) {
            adapter.removeMessage()
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
            adapter.removeMessage()
            adapter.addProgressFooter()
        }
    }

    private fun initRecyclerView() {
        columnsCount = AndroidUtils.calculateNoOfColumns(activity!!, PHOTO_ADAPTER_VIEW_WIDTH)

        adapter = ReceivedPhotosAdapter(activity!!, imageLoader, photoClickSubject)
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
        adapter.runOnAdapterHandler {
            adapter.notifyDataSetChanged()
            receivedPhotosList.scrollToPosition(0)
        }
    }

    private fun onPhotoAnswerClick(photoClick: ReceivedPhotosAdapter.PhotoAnswerClick) {
        when (photoClick) {
            is ReceivedPhotosAdapter.PhotoAnswerClick.ShowReceiverLocation -> {
                val photo = photoClick.photo
                if (photo.isAnonymous()) {
                    Toast.makeText(activity, getString(R.string.photo_sent_anonymously_msg), Toast.LENGTH_LONG).show()
                    return
                }

                val intent = Intent(activity, MapActivity::class.java)
                intent.putExtra("lon", photo.lon)
                intent.putExtra("lat", photo.lat)

                startActivity(intent)
            }

            is ReceivedPhotosAdapter.PhotoAnswerClick.ShowFullPhoto -> {
                val photo = photoClick.photo
                val intent = Intent(activity, ViewPhotoFullSizeActivity::class.java)
                intent.putExtra("photo_name", photo.photoName)

                startActivity(intent)
            }

            is ReceivedPhotosAdapter.PhotoAnswerClick.DeletePhoto -> {
                getViewModel().inputs.showDeletePhotoConfirmationDialog(photoClick.photo.photoName)
            }

            is ReceivedPhotosAdapter.PhotoAnswerClick.ReportPhoto -> {
                TODO()
            }
        }
    }

    private fun onDeletePhotoConfirmed(photoName: String) {
        getViewModel().inputs.deletePhotoAnswerFromDatabase(photoName)
    }

    private fun onLookingForPhotoState(state: LookingForPhotoState) {
        when (state) {
            is LookingForPhotoState.UploadMorePhotos -> {
                Timber.tag(ttag).d("onLookingForPhotoState() LookingForPhotoState.UploadMorePhotos")

                adapter.runOnAdapterHandler {
                    adapter.removeLookingForPhotoIndicator()
                    adapter.removeMessage()

                    adapter.addMessage(ReceivedPhotosAdapter.MESSAGE_TYPE_UPLOAD_MORE_PHOTOS)
                }
            }

            is LookingForPhotoState.LocalRepositoryError -> {
                Timber.tag(ttag).d("onLookingForPhotoState() LookingForPhotoState.LocalRepositoryError")

                adapter.runOnAdapterHandler {
                    adapter.removeLookingForPhotoIndicator()
                    adapter.removeMessage()

                    adapter.addMessage(ReceivedPhotosAdapter.MESSAGE_TYPE_ERROR)
                }
            }

            is LookingForPhotoState.ServerHasNoPhotos -> {
                Timber.tag(ttag).d("onLookingForPhotoState() LookingForPhotoState.ServerHasNoPhotos")

                adapter.runOnAdapterHandler {
                    adapter.removeLookingForPhotoIndicator()
                    adapter.removeMessage()

                    adapter.addLookingForPhotoIndicator()
                }
            }

            is LookingForPhotoState.PhotoFound -> {
                Timber.tag(ttag).d("onLookingForPhotoState() LookingForPhotoState.PhotoFound")

                adapter.runOnAdapterHandler {
                    if (state.allFound) {
                        adapter.removeLookingForPhotoIndicator()
                    }

                    adapter.removeMessage()
                    adapter.addFirst(AdapterItem(state.photoAnswer, AdapterItemType.VIEW_ITEM))
                }
            }

            else -> IllegalArgumentException("Bad value")
        }
    }

    private fun startLookingForPhotos() {
        Timber.tag(ttag).d("startLookingForPhotos() Showing scheduleLookingForPhotoAnswer")

        //TODO: rewrite
        (activity as AllPhotosViewActivity).scheduleLookingForPhotoAnswer()
        showLookingForPhotoIndicator()
    }

    private fun onUploadMorePhotos() {
        Timber.tag(ttag).d("onUploadMorePhotos()")

        adapter.runOnAdapterHandler {
            adapter.addMessage(ReceivedPhotosAdapter.MESSAGE_TYPE_UPLOAD_MORE_PHOTOS)
        }
    }

    private fun onDeletePhotoAnswerFromDatabase(photoName: String) {
        Timber.tag(ttag).d("onDeletePhotoAnswerFromDatabase()")

        adapter.runOnAdapterHandler {
            adapter.removePhotoAnswer(photoName)
        }
    }

    private fun onUnknownError(error: Throwable) {
        //TODO: rewrite
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
