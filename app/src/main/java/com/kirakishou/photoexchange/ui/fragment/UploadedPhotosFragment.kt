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
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoState
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
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject


class UploadedPhotosFragment : BaseFragment(), StateEventListener<UploadedPhotosFragmentEvent> {

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
    private val failedToUploadPhotoButtonClicksSubject = PublishSubject.create<UploadedPhotosAdapter.UploadedPhotosAdapterButtonClick>().toSerialized()
    private var viewState = UploadedPhotosFragmentViewState()
    private val loadMoreSubject = PublishSubject.create<Int>()
    private var photosPerPage = 0
    private var lastId = Long.MAX_VALUE

    override fun getContentView(): Int = R.layout.fragment_uploaded_photos

    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
        initRx()
        initRecyclerView()
        loadNotYetUploadedPhotos()

        if (savedInstanceState != null) {
            restoreFragmentFromViewState(savedInstanceState)
        } else {

        }
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

        adapter = UploadedPhotosAdapter(requireContext(), imageLoader, failedToUploadPhotoButtonClicksSubject)
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

        compositeDisposable += viewModel.eventForwarder.getUploadedPhotosFragmentEventsStream()
            .observeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { viewState -> onStateEvent(viewState) }
            .doOnError { Timber.tag(TAG).e(it) }
            .subscribe()

        compositeDisposable += failedToUploadPhotoButtonClicksSubject
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { Timber.tag(TAG).e(it) }
            .subscribe({ })

        compositeDisposable += loadMoreSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(Schedulers.io())
            .concatMap { viewModel.loadNextPageOfUploadedPhotos(lastId, photosPerPage) }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { photos -> addUploadedPhotosToAdapter(photos) }
            .doOnError { Timber.tag(TAG).e(it) }
            .subscribe()
    }

    private fun loadNotYetUploadedPhotos() {
        compositeDisposable += viewModel.loadMyPhotos()
            .flatMapObservable { takenPhotos ->
                Observable.just(takenPhotos)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext { photos -> addTakenPhotosToAdapter(photos) }
            }
            .observeOn(Schedulers.io())
            .flatMap {
                return@flatMap viewModel.checkHasPhotosToUpload()
                    .flatMap { hasPhotosToUpload ->
                        if (hasPhotosToUpload) {
                            viewModel.eventForwarder.sendPhotoActivityEvent(PhotosActivityEvent.StartUploadingService())
                            return@flatMap Observable.empty<List<UploadedPhoto>>()
                        }

                        return@flatMap viewModel.loadNextPageOfUploadedPhotos(lastId, photosPerPage)
                    }
            }
            .subscribe({ uploadedPhotosList ->
                addUploadedPhotosToAdapter(uploadedPhotosList)
            }, { error ->
                Timber.tag(TAG).e(error)
            })
    }

    override fun onStateEvent(event: UploadedPhotosFragmentEvent) {
        if (!isAdded) {
            return
        }

        uploadedPhotosList.post {
            when (event) {
                is UploadedPhotosFragmentEvent.UiEvents -> {
                    onUiEvent(event)
                }

                is UploadedPhotosFragmentEvent.PhotoUploadEvent -> {
                    onUploadingEvent(event)
                }
            }
        }
    }

    private fun onUiEvent(event: UploadedPhotosFragmentEvent.UiEvents) {
        when (event) {
            is UploadedPhotosFragmentEvent.UiEvents.ScrollToTop -> {
                uploadedPhotosList.scrollToPosition(0)
            }
            is UploadedPhotosFragmentEvent.UiEvents.ShowObtainCurrentLocationNotification -> {
                adapter.showObtainCurrentLocationNotification()
            }
            is UploadedPhotosFragmentEvent.UiEvents.HideObtainCurrentLocationNotification -> {
                adapter.hideObtainCurrentLocationNotification()
            }
            is UploadedPhotosFragmentEvent.UiEvents.ShowProgressFooter -> {
                addProgressFooter()
            }
            is UploadedPhotosFragmentEvent.UiEvents.HideProgressFooter -> {
                removeProgressFooter()
            }
            is UploadedPhotosFragmentEvent.UiEvents.RemovePhoto -> {
                adapter.removePhotoById(event.photo.id)
            }
            is UploadedPhotosFragmentEvent.UiEvents.AddPhoto -> {
                adapter.addTakenPhoto(event.photo)
            }
            else -> throw IllegalArgumentException("Unknown UploadedPhotosFragmentEvent $event")
        }
    }

    private fun onUploadingEvent(event: UploadedPhotosFragmentEvent.PhotoUploadEvent) {
        if (!isAdded) {
            return
        }

        uploadedPhotosList.post {
            when (event) {
                is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnLocationUpdateStart -> {
                    adapter.showObtainCurrentLocationNotification()
                }
                is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnLocationUpdateEnd -> {
                    adapter.hideObtainCurrentLocationNotification()
                }
                is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPrepare -> {

                }
                is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadStart -> {
                    adapter.addTakenPhoto(event.photo.also { it.photoState = PhotoState.PHOTO_UPLOADING })
                }
                is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress -> {
                    adapter.addTakenPhoto(event.photo)
                    adapter.updatePhotoProgress(event.photo.id, event.progress)
                }
                is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnUploaded -> {
                    adapter.removePhotoById(event.photo.photoId)
                    adapter.addUploadedPhoto(event.photo)
                }
                is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnFailedToUpload -> {
                    adapter.removePhotoById(event.photo.id)
                    adapter.addTakenPhoto(event.photo.also { it.photoState = PhotoState.FAILED_TO_UPLOAD })
                    (requireActivity() as PhotosActivity).showUploadPhotoErrorMessage(event.errorCode)
                }
                is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnFoundPhotoAnswer -> {
                    adapter.updateUploadedPhotoSetReceiverInfo(event.takenPhotoName)
                }
                is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnEnd -> {
                    viewModel.eventForwarder.sendPhotoActivityEvent(PhotosActivityEvent.StartReceivingService())
                }

                is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnCouldNotGetUserIdFromServerError,
                is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnUnknownError -> {
                    handleErrorEvent(event)
                }
                else -> throw IllegalArgumentException("Unknown PhotoUploadEvent $event")
            }
        }
    }

    private fun handleErrorEvent(event: UploadedPhotosFragmentEvent.PhotoUploadEvent) {
        when (event) {
            is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnCouldNotGetUserIdFromServerError -> {
                Timber.tag(TAG).e("Could not get user photoId from the server")
                showToast("Could not get user photoId from the server")
            }
            is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnUnknownError -> {
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
