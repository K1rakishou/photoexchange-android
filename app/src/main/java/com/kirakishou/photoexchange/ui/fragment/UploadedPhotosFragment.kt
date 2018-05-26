package com.kirakishou.photoexchange.ui.fragment

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.Toast
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.extension.filterErrorCodes
import com.kirakishou.photoexchange.helper.intercom.StateEventListener
import com.kirakishou.photoexchange.helper.intercom.event.BaseEvent
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.PhotoUploadEvent
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.adapter.UploadedPhotosAdapter
import com.kirakishou.photoexchange.ui.adapter.UploadedPhotosAdapterSpanSizeLookup
import com.kirakishou.photoexchange.ui.viewstate.UploadedPhotosFragmentViewState
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.ui.widget.EndlessRecyclerOnScrollListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class UploadedPhotosFragment : BaseFragment(), StateEventListener {

    @BindView(R.id.my_photos_list)
    lateinit var uploadedPhotosList: RecyclerView

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var viewModel: PhotosActivityViewModel

    lateinit var adapter: UploadedPhotosAdapter
    lateinit var endlessScrollListener: EndlessRecyclerOnScrollListener

    private val TAG = "UploadedPhotosFragment"
    private val PHOTO_ADAPTER_VIEW_WIDTH = 288
    private val EVENT_FORWARDING_DELAY = 150L
    private val adapterButtonsClickSubject = PublishSubject.create<UploadedPhotosAdapter.UploadedPhotosAdapterButtonClickEvent>().toSerialized()
    private var viewState = UploadedPhotosFragmentViewState()
    private val loadMoreSubject = PublishSubject.create<Int>()
    private var photosPerPage = 0
    private var lastId = Long.MAX_VALUE

    override fun getContentView(): Int = R.layout.fragment_uploaded_photos

    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
        initRx()
        initRecyclerView()
        loadPhotos()

        restoreFragmentFromViewState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewState.saveToBundle(outState)
    }

    private fun restoreFragmentFromViewState(savedInstanceState: Bundle?) {
        viewState = UploadedPhotosFragmentViewState()
            .also { it.loadFromBundle(savedInstanceState) }
    }

    override fun onFragmentViewDestroy() {
    }

    private fun initRecyclerView() {
        val columnsCount = AndroidUtils.calculateNoOfColumns(requireContext(), PHOTO_ADAPTER_VIEW_WIDTH)

        adapter = UploadedPhotosAdapter(requireContext(), imageLoader, adapterButtonsClickSubject)
        adapter.init()

        val layoutManager = GridLayoutManager(requireContext(), columnsCount)
        layoutManager.spanSizeLookup = UploadedPhotosAdapterSpanSizeLookup(adapter, columnsCount)
        photosPerPage = Constants.UPLOADED_PHOTOS_PER_ROW * layoutManager.spanCount

        endlessScrollListener = EndlessRecyclerOnScrollListener(layoutManager, photosPerPage, loadMoreSubject)

        uploadedPhotosList.layoutManager = layoutManager
        uploadedPhotosList.adapter = adapter
        uploadedPhotosList.clearOnScrollListeners()
        uploadedPhotosList.addOnScrollListener(endlessScrollListener)
    }

    private fun initRx() {
        compositeDisposable += viewModel.errorCodesSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .filterErrorCodes(UploadedPhotosFragment::class.java)
            .filter { isVisible }
            .doOnNext { handleError(it) }
            .subscribe()

//        compositeDisposable += viewModel.onPhotoUploadEventSubject
//            .observeOn(AndroidSchedulers.mainThread())
//            .flatMap { event ->
//                return@flatMap if (event is PhotoUploadEvent.OnProgress) {
//                    //do not add any delay if event is PhotoUploadEvent.OnProgress
//                    Observable.just(event)
//                } else {
//                    //add slight delay before doing anything with adapter because
//                    //it will flicker if events come very fast
//                    Observable.just(event)
//                        .zipWith(Observable.timer(EVENT_FORWARDING_DELAY, TimeUnit.MILLISECONDS))
//                        .map { it.first }
//                }
//            }
//            .doOnNext { event -> onUploadingEvent(event) }
//            .doOnError { Timber.tag(TAG).e(it) }
//            .subscribe()

        compositeDisposable += viewModel.eventForwarder.getUploadedPhotosFragmentEventsStream()
            .observeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { viewState -> onStateEvent(viewState) }
            .doOnError { Timber.tag(TAG).e(it) }
            .subscribe()

        compositeDisposable += adapterButtonsClickSubject
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { Timber.tag(TAG).e(it) }
            .subscribe(viewModel.uploadedPhotosAdapterButtonClickSubject::onNext)

        compositeDisposable += loadMoreSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(Schedulers.io())
            .concatMap { viewModel.loadNextPageOfUploadedPhotos(lastId, photosPerPage) }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { photos -> addUploadedPhotosToAdapter(photos) }
            .doOnError { Timber.tag(TAG).e(it) }
            .subscribe()
    }

    private fun loadPhotos() {
        compositeDisposable += viewModel.loadMyPhotos()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { photos -> addTakenPhotosToAdapter(photos) }
            .toObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .observeOn(Schedulers.io())
            .concatMap { viewModel.loadNextPageOfUploadedPhotos(lastId, photosPerPage) }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { photos -> addUploadedPhotosToAdapter(photos) }
            .doOnError { Timber.tag(TAG).e(it) }
            .subscribe()
    }

    override fun onStateEvent(event: BaseEvent) {
        if (!isAdded) {
            return
        }

        uploadedPhotosList.post {
            when (event) {
                is UploadedPhotosFragmentEvent -> {
                    onFragmentEvent(event)
                }

                is PhotoUploadEvent -> {
                    onUploadingEvent(event)
                }
            }
        }
    }

    private fun onFragmentEvent(event: UploadedPhotosFragmentEvent) {
        when (event) {
            is UploadedPhotosFragmentEvent.ScrollToTop -> {
                uploadedPhotosList.scrollToPosition(0)
            }
            is UploadedPhotosFragmentEvent.ShowObtainCurrentLocationNotification -> {
                adapter.showObtainCurrentLocationNotification()
            }
            is UploadedPhotosFragmentEvent.HideObtainCurrentLocationNotification -> {
                adapter.hideObtainCurrentLocationNotification()
            }
            is UploadedPhotosFragmentEvent.ShowProgressFooter -> {
                addProgressFooter()
            }
            is UploadedPhotosFragmentEvent.HideProgressFooter -> {
                removeProgressFooter()
            }
            is UploadedPhotosFragmentEvent.RemovePhoto -> {
                adapter.removePhotoById(event.photo.id)
            }
            is UploadedPhotosFragmentEvent.AddPhoto -> {
                adapter.addTakenPhoto(event.photo)
            }
            else -> throw IllegalArgumentException("Unknown UploadedPhotosFragmentEvent $event")
        }
    }

    private fun onUploadingEvent(event: PhotoUploadEvent) {
        if (!isAdded) {
            return
        }

        uploadedPhotosList.post {
            when (event) {
                is PhotoUploadEvent.OnLocationUpdateStart -> {
                    adapter.showObtainCurrentLocationNotification()
                }
                is PhotoUploadEvent.OnLocationUpdateEnd -> {
                    adapter.hideObtainCurrentLocationNotification()
                }
                is PhotoUploadEvent.OnPrepare -> {

                }
                is PhotoUploadEvent.OnPhotoUploadStart -> {
                    adapter.addTakenPhoto(event.photo.also { it.photoState = PhotoState.PHOTO_UPLOADING })
                }
                is PhotoUploadEvent.OnProgress -> {
                    adapter.addTakenPhoto(event.photo)
                    adapter.updatePhotoProgress(event.photo.id, event.progress)
                }
                is PhotoUploadEvent.OnUploaded -> {
                    adapter.removePhotoById(event.photo.photoId)
                    adapter.addUploadedPhoto(event.photo)
                }
                is PhotoUploadEvent.OnFailedToUpload -> {
                    adapter.removePhotoById(event.photo.id)
                    adapter.addTakenPhoto(event.photo.also { it.photoState = PhotoState.FAILED_TO_UPLOAD })
                    (requireActivity() as PhotosActivity).showUploadPhotoErrorMessage(event.errorCode)
                }
                is PhotoUploadEvent.OnFoundPhotoAnswer -> {
                    adapter.updateUploadedPhotoSetReceiverInfo(event.takenPhotoId)
                }
                is PhotoUploadEvent.OnEnd -> {
                }

                is PhotoUploadEvent.OnCouldNotGetUserIdFromServerError,
                is PhotoUploadEvent.OnUnknownError -> {
                    handleErrorEvent(event)
                }
                else -> throw IllegalArgumentException("Unknown PhotoUploadEvent $event")
            }
        }
    }

    private fun handleErrorEvent(event: PhotoUploadEvent) {
        when (event) {
            is PhotoUploadEvent.OnCouldNotGetUserIdFromServerError -> {
                Timber.tag(TAG).e("Could not get user photoId from the server")
                showToast("Could not get user photoId from the server")
            }
            is PhotoUploadEvent.OnUnknownError -> {
                (requireActivity() as PhotosActivity).showUnknownErrorMessage(event.error)
            }
            else -> IllegalStateException("Unknown event $event")
        }

        adapter.updateAllPhotosState(PhotoState.FAILED_TO_UPLOAD)
    }

    private fun addUploadedPhotosToAdapter(uploadedPhotos: List<UploadedPhoto>) {
        if (!isAdded) {
            return
        }

        uploadedPhotosList.post {
            endlessScrollListener.pageLoaded()

            if (uploadedPhotos.isNotEmpty()) {
                lastId = uploadedPhotos.last().photoId
                adapter.addUploadedPhotos(uploadedPhotos)
            }

            if (uploadedPhotos.size < photosPerPage) {
                endlessScrollListener.reachedEnd()
            }
        }
    }

    private fun addTakenPhotosToAdapter(takenPhotos: List<TakenPhoto>) {
        if (!isAdded) {
            return
        }

        uploadedPhotosList.post {
            if (takenPhotos.isNotEmpty()) {
                adapter.clear()
                adapter.addTakenPhotos(takenPhotos)
            } else {
                //TODO: show notification that no photos has been uploaded yet
            }
        }
    }

    private fun addProgressFooter() {
        uploadedPhotosList.post {
            adapter.showProgressFooter()
        }
    }

    private fun removeProgressFooter() {
        uploadedPhotosList.post {
            adapter.hideProgressFooter()
        }
    }

    private fun handleError(errorCode: ErrorCode) {
        when (errorCode) {
            is ErrorCode.GetUploadedPhotosErrors.LocalUserIdIsEmpty -> {
                removeProgressFooter()
            }
        }

        (requireActivity() as PhotosActivity).showErrorCodeToast(errorCode)
    }

    private fun showToast(message: String, duration: Int = Toast.LENGTH_LONG) {
        (requireActivity() as PhotosActivity).showToast(message, duration)
    }

    override fun resolveDaggerDependency() {
        (requireActivity() as PhotosActivity).activityComponent
            .inject(this)
    }

    companion object {
        fun newInstance(): UploadedPhotosFragment {
            val fragment = UploadedPhotosFragment()
            val args = Bundle()

            fragment.arguments = args
            return fragment
        }
    }
}
