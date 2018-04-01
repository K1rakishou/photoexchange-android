package com.kirakishou.photoexchange.ui.fragment

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.PhotoUploadingEvent
import com.kirakishou.photoexchange.mvp.viewmodel.AllPhotosActivityViewModel
import com.kirakishou.photoexchange.ui.activity.AllPhotosActivity
import com.kirakishou.photoexchange.ui.adapter.MyPhotosAdapter
import com.kirakishou.photoexchange.ui.adapter.MyPhotosAdapterItem
import com.kirakishou.photoexchange.ui.viewstate.MyPhotosFragmentViewState
import com.kirakishou.photoexchange.ui.adapter.MyPhotosAdapterSpanSizeLookup
import com.kirakishou.photoexchange.ui.dialog.CancelAllFailedToUploadPhotosDialog
import com.kirakishou.photoexchange.ui.dialog.CancelAllQueuedUpPhotosDialog
import com.kirakishou.photoexchange.ui.dialog.CancelPhotoUploadingDialog
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.zipWith
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject


class MyPhotosFragment : BaseFragment() {

    @BindView(R.id.my_photos_list)
    lateinit var myPhotosList: RecyclerView

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var viewModel: AllPhotosActivityViewModel

    private val PHOTO_ADAPTER_VIEW_WIDTH = 288
    private val adapterButtonClickSubject = PublishSubject.create<MyPhotosAdapter.AdapterButtonClickEvent>().toSerialized()

    lateinit var adapter: MyPhotosAdapter

    override fun getContentView(): Int = R.layout.fragment_my_photos

    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
        initRx()
        initRecyclerView()

        loadUploadedPhotos()

        if (savedInstanceState == null) {
            showFailedToUploadPhotosNotification()
        } else {
            showQueuedUpPhotosNotification()
        }
    }

    private fun loadUploadedPhotos() {
        compositeDisposable += viewModel.loadUploadedPhotosFromDatabase()
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { photos -> onUploadedPhotosLoadedFromDatabase(photos) }
            .subscribe()
    }

    private fun showQueuedUpPhotosNotification() {
        compositeDisposable += viewModel.countQueuedUpPhotos()
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .filter { count -> count > 0 }
            .doOnSuccess { count -> adapter.showQueuedUpPhotosCountNotification(count) }
            .subscribe()
    }

    private fun showFailedToUploadPhotosNotification() {
        compositeDisposable += viewModel.countFailedToUploadPhotos()
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .filter { count -> count > 0 }
            .doOnSuccess { count -> adapter.showFailedToUploadPhotosNotification(count) }
            .subscribe()
    }

    override fun onFragmentViewDestroy() {
        viewModel.resumeUploadingProcess()
    }

    private fun initRecyclerView() {
        val columnsCount = AndroidUtils.calculateNoOfColumns(activity!!, PHOTO_ADAPTER_VIEW_WIDTH)

        adapter = MyPhotosAdapter(activity!!, imageLoader, adapterButtonClickSubject)
        adapter.init()

        val layoutManager = GridLayoutManager(activity!!, columnsCount)
        layoutManager.spanSizeLookup = MyPhotosAdapterSpanSizeLookup(adapter, columnsCount)

        myPhotosList.layoutManager = layoutManager
        myPhotosList.adapter = adapter
    }

    private fun initRx() {
        compositeDisposable += adapterButtonClickSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { viewModel.stopUploadingProcess() }
            .flatMap { buttonClickEvent -> askUserConfirmation(buttonClickEvent) }
            .filter { (cancelOperation, _) -> !cancelOperation }
            .map { (_, buttonClickEvent) -> buttonClickEvent }
            .doOnNext(viewModel.adapterButtonClickSubject::onNext)
            .doOnError(viewModel.adapterButtonClickSubject::onError)
            .subscribe()

        compositeDisposable += viewModel.onUploadingPhotoEventSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { event -> onUploadingEvent(event) }
            .subscribe()

        compositeDisposable += viewModel.myPhotosFragmentViewStateSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { viewState -> onViewStateChanged(viewState) }
            .subscribe()
    }

    private fun askUserConfirmation(buttonClickEvent: MyPhotosAdapter.AdapterButtonClickEvent): Observable<out Pair<Boolean, MyPhotosAdapter.AdapterButtonClickEvent>> {
        when (buttonClickEvent) {
            is MyPhotosAdapter.AdapterButtonClickEvent.CancelAllFailedToUploadPhotosButtonClick -> {
                return CancelAllFailedToUploadPhotosDialog()
                    .show(activity!!)
                    .doOnNext { positive ->
                        if (positive) {
                            viewModel.deleteAllWithState(PhotoState.FAILED_TO_UPLOAD).blockingAwait()
                            activity?.runOnUiThread {
                                adapter.hideFailedToUploadPhotosNotification()
                            }
                        }
                    }
                    .zipWith(Observable.just(buttonClickEvent))
            }
            is MyPhotosAdapter.AdapterButtonClickEvent.RetryToUploadPhotosButtonClick -> {
                return Observable.just(Pair(true, buttonClickEvent))
                    .doOnNext { _ ->
                        viewModel.changePhotosStates(PhotoState.FAILED_TO_UPLOAD, PhotoState.PHOTO_QUEUED_UP).blockingAwait()
                        activity?.runOnUiThread {
                            adapter.hideFailedToUploadPhotosNotification()
                        }
                    }
            }
            is MyPhotosAdapter.AdapterButtonClickEvent.CancelAllQueuedUpPhotosButtonClick -> {
                return CancelAllQueuedUpPhotosDialog()
                    .show(activity!!)
                    .doOnNext { positive ->
                        if (positive) {
                            viewModel.deleteAllWithState(PhotoState.PHOTO_QUEUED_UP).blockingAwait()
                            activity?.runOnUiThread {
                                adapter.hideQueuedUpPhotosCountNotification()
                            }
                        }
                    }
                    .zipWith(Observable.just(buttonClickEvent))
            }
            is MyPhotosAdapter.AdapterButtonClickEvent.CancelPhotoUploading -> {
                return CancelPhotoUploadingDialog()
                    .show(activity!!)
                    .doOnNext { positive ->
                        if (positive) {
                            viewModel.deleteByIdAndState(buttonClickEvent.photoId, buttonClickEvent.photoState).blockingAwait()
                            activity?.runOnUiThread {
                                adapter.removePhotoById(buttonClickEvent.photoId)
                            }
                        }
                    }
                    .zipWith(Observable.just(buttonClickEvent))
            }
            else -> {
                throw IllegalArgumentException("Unknown buttonClickEvent ${buttonClickEvent::class.java}")
            }
        }
    }

    private fun onViewStateChanged(viewState: MyPhotosFragmentViewState) {
        if (!isAdded) {
            return
        }

        activity?.runOnUiThread {
            when (viewState) {
                is MyPhotosFragmentViewState.Default -> {

                }
                is MyPhotosFragmentViewState.ShowObtainCurrentLocationNotification -> {
                    if (viewState.show) {
                        adapter.showObtainCurrentLocationNotification()
                    } else {
                        adapter.hideObtainCurrentLocationNotification()
                    }
                }
            }
        }
    }

    private fun scrollRecyclerViewToTop() {
        val layoutManager = (myPhotosList.layoutManager as GridLayoutManager)
        if (layoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
            myPhotosList.scrollToPosition(0)
        }
    }

    private fun onUploadingEvent(event: PhotoUploadingEvent) {
        if (!isAdded) {
            return
        }

        activity?.runOnUiThread {
            when (event) {
                is PhotoUploadingEvent.OnPrepare -> {
                    scrollRecyclerViewToTop()
                    adapter.updateQueuedUpPhotosCountNotification(event.queuedUpPhotosCount)
                }
                is PhotoUploadingEvent.OnPhotoUploadingStart -> {
                    adapter.updateQueuedUpPhotosCountNotification(event.queuedUpPhotosCount)
                    adapter.add(0, MyPhotosAdapterItem.MyPhotoItem(event.myPhoto))
                }
                is PhotoUploadingEvent.OnProgress -> {
                    adapter.updatePhotoProgress(event.photoId, event.progress)
                }
                is PhotoUploadingEvent.OnUploaded -> {
                    adapter.updatePhotoState(event.myPhoto.id, event.myPhoto.photoState)
                }
                is PhotoUploadingEvent.OnEnd -> {
                    adapter.hideQueuedUpPhotosCountNotification()
                    showFailedToUploadPhotosNotification()
                }
                is PhotoUploadingEvent.OnFailedToUpload -> {
                    adapter.removePhotoById(event.myPhoto.id)
                }
                is PhotoUploadingEvent.OnUnknownError -> {
                    adapter.clear()
                    loadUploadedPhotos()
                    showFailedToUploadPhotosNotification()
                }
            }
        }
    }

    private fun onUploadedPhotosLoadedFromDatabase(uploadedPhotos: List<MyPhoto>) {
        activity?.runOnUiThread {
            if (uploadedPhotos.isNotEmpty()) {
                val mapped = uploadedPhotos.map { MyPhotosAdapterItem.MyPhotoItem(it) }
                adapter.addAll(mapped)
            } else {
                //TODO: show notification that no photos has been uploaded yet
            }
        }
    }

    override fun resolveDaggerDependency() {
        (activity as AllPhotosActivity).activityComponent
            .inject(this)
    }

    companion object {
        fun newInstance(): MyPhotosFragment {
            val fragment = MyPhotosFragment()
            val args = Bundle()

            fragment.arguments = args
            return fragment
        }
    }
}
