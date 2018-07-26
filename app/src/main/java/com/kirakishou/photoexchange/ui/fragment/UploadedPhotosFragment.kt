package com.kirakishou.photoexchange.ui.fragment

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.Toast
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.database.entity.CachedPhotoIdEntity
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.IntercomListener
import com.kirakishou.photoexchange.helper.intercom.StateEventListener
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.model.other.Constants.DEFAULT_ADAPTER_ITEM_WIDTH
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.adapter.UploadedPhotosAdapter
import com.kirakishou.photoexchange.ui.adapter.UploadedPhotosAdapterSpanSizeLookup
import com.kirakishou.photoexchange.ui.widget.EndlessRecyclerOnScrollListener
import com.kirakishou.photoexchange.ui.widget.SwipeToRefreshLayout
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject


class UploadedPhotosFragment : BaseFragment(), StateEventListener<UploadedPhotosFragmentEvent>, IntercomListener {

    @BindView(R.id.my_photos_list)
    lateinit var uploadedPhotosList: RecyclerView

    @BindView(R.id.swipe_refresh_layout)
    lateinit var swipeToRefreshLayout: SwipeToRefreshLayout

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var viewModel: PhotosActivityViewModel

    lateinit var adapter: UploadedPhotosAdapter
    lateinit var endlessScrollListener: EndlessRecyclerOnScrollListener

    private val TAG = "UploadedPhotosFragment"
    private val PHOTO_ADAPTER_VIEW_WIDTH = DEFAULT_ADAPTER_ITEM_WIDTH
    private val DISTANCE_TO_TRIGGER_SYNC = 256
    private val failedToUploadPhotoButtonClicksSubject = PublishSubject.create<UploadedPhotosAdapter.UploadedPhotosAdapterButtonClick>().toSerialized()

    override fun getContentView(): Int = R.layout.fragment_uploaded_photos

    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
        viewModel.uploadedPhotosFragmentIsFreshlyCreated.onNext(savedInstanceState == null)

        initRx()
        initViews()
    }

    override fun onFragmentViewDestroy() {
    }

    private fun initRx() {
        compositeDisposable += viewModel.uploadedPhotosFragmentErrorCodeSubject
            .subscribe({ handleKnownErrors(it) })

        compositeDisposable += viewModel.intercom.uploadedPhotosFragmentEvents.listen()
            .doOnNext { viewState -> onStateEvent(viewState) }
            .doOnError { Timber.tag(TAG).e(it) }
            .subscribe()

        compositeDisposable += failedToUploadPhotoButtonClicksSubject
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                viewModel.intercom.tell<PhotosActivity>()
                    .that(PhotosActivityEvent.FailedToUploadPhotoButtonClicked(it))
            }, { Timber.tag(TAG).e(it) })

        compositeDisposable += lifecycle.getLifecycle()
            .subscribe(viewModel.uploadedPhotosFragmentLifecycle::onNext)
    }

    private fun initViews() {
        val columnsCount = AndroidUtils.calculateNoOfColumns(requireContext(), PHOTO_ADAPTER_VIEW_WIDTH)

        adapter = UploadedPhotosAdapter(requireContext(), imageLoader, failedToUploadPhotoButtonClicksSubject)

        val layoutManager = GridLayoutManager(requireContext(), columnsCount)
        layoutManager.spanSizeLookup = UploadedPhotosAdapterSpanSizeLookup(adapter, columnsCount)

        val photosPerPage = Constants.UPLOADED_PHOTOS_PER_ROW * layoutManager.spanCount
        viewModel.uploadedPhotosFragmentViewState.update(photosPerPage = photosPerPage)
        endlessScrollListener = EndlessRecyclerOnScrollListener(TAG, layoutManager, photosPerPage, viewModel.uploadedPhotosFragmentLoadPhotosSubject)

        uploadedPhotosList.layoutManager = layoutManager
        uploadedPhotosList.adapter = adapter
        uploadedPhotosList.clearOnScrollListeners()
        uploadedPhotosList.addOnScrollListener(endlessScrollListener)

        swipeToRefreshLayout.setDistanceToTriggerSync(DISTANCE_TO_TRIGGER_SYNC)
        swipeToRefreshLayout.setOnRefreshListener {
            endlessScrollListener.stopLoading()
            viewModel.uploadedPhotosFragmentRefreshPhotos.onNext(Unit)
        }
    }

    private fun startLoadingFirstPageOfPhotos() {
        viewModel.uploadedPhotosFragmentLoadPhotosSubject.onNext(false)
    }

    private fun startLoadingUploadedPhotosWhenPhotoWasDeleted() {
        if (adapter.getFailedPhotosCount() == 0) {
            viewModel.uploadedPhotosFragmentLoadPhotosSubject.onNext(false)
        }
    }

    override fun onStateEvent(event: UploadedPhotosFragmentEvent) {
        if (!isAdded) {
            return
        }

        uploadedPhotosList.post {
            when (event) {
                is UploadedPhotosFragmentEvent.GeneralEvents -> onUiEvent(event)
                is UploadedPhotosFragmentEvent.PhotoUploadEvent -> onUploadingEvent(event)
            }.safe
        }
    }

    private fun onUiEvent(event: UploadedPhotosFragmentEvent.GeneralEvents) {
        if (!isAdded) {
            return
        }

        uploadedPhotosList.post {
            when (event) {
                is UploadedPhotosFragmentEvent.GeneralEvents.ScrollToTop -> {
                    uploadedPhotosList.scrollToPosition(0)
                }
                is UploadedPhotosFragmentEvent.GeneralEvents.ShowProgressFooter -> {
                    showProgressFooter()
                }
                is UploadedPhotosFragmentEvent.GeneralEvents.HideProgressFooter -> {
                    hideProgressFooter()
                }
                is UploadedPhotosFragmentEvent.GeneralEvents.RemovePhoto -> {
                    adapter.removePhotoById(event.photo.id)
                }
                is UploadedPhotosFragmentEvent.GeneralEvents.AddPhoto -> {
                    adapter.addTakenPhoto(event.photo)
                }
                is UploadedPhotosFragmentEvent.GeneralEvents.AddFailedToUploadPhotos -> {
                    addTakenPhotosToAdapter(event.photos)
                }
                is UploadedPhotosFragmentEvent.GeneralEvents.AddQueuedUpPhotos -> {
                    addTakenPhotosToAdapter(event.photos)
                }
                is UploadedPhotosFragmentEvent.GeneralEvents.AddUploadedPhotos -> {
                    addUploadedPhotosToAdapter(event.photos)
                }
                is UploadedPhotosFragmentEvent.GeneralEvents.PhotoRemoved -> {
                    startLoadingUploadedPhotosWhenPhotoWasDeleted()
                }
                is UploadedPhotosFragmentEvent.GeneralEvents.AfterPermissionRequest -> {
                    startLoadingFirstPageOfPhotos()
                }
                is UploadedPhotosFragmentEvent.GeneralEvents.UpdateReceiverInfo -> {
                    event.receivedPhotos.forEach {
                        adapter.updateUploadedPhotoSetReceiverInfo(it.uploadedPhotoName)
                    }
                }
                is UploadedPhotosFragmentEvent.GeneralEvents.DisableEndlessScrolling -> endlessScrollListener.stopLoading()
                is UploadedPhotosFragmentEvent.GeneralEvents.EnableEndlessScrolling -> endlessScrollListener.startLoading()
                is UploadedPhotosFragmentEvent.GeneralEvents.ClearAdapter -> adapter.clear()
                is UploadedPhotosFragmentEvent.GeneralEvents.StartRefreshing -> swipeToRefreshLayout.isRefreshing = true
                is UploadedPhotosFragmentEvent.GeneralEvents.StopRefreshing -> swipeToRefreshLayout.isRefreshing = false
                is UploadedPhotosFragmentEvent.GeneralEvents.ClearCache -> clearIdsCache()
            }.safe
        }
    }

    private fun clearIdsCache() {
        compositeDisposable += viewModel.clearPhotoIdsCache(CachedPhotoIdEntity.PhotoType.UploadedPhoto)
            .subscribe()
    }

    private fun onUploadingEvent(event: UploadedPhotosFragmentEvent.PhotoUploadEvent) {
        if (!isAdded) {
            return
        }

        uploadedPhotosList.post {
            when (event) {
                is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadStart -> {
                    adapter.addTakenPhoto(event.photo.also { it.photoState = PhotoState.PHOTO_UPLOADING })
                }
                is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress -> {
                    adapter.addTakenPhoto(event.photo)
                    adapter.updatePhotoProgress(event.photo.id, event.progress)
                }
                is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnUploaded -> {
                    adapter.removePhotoById(event.takenPhoto.id)
                }
                is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnFailedToUpload -> {
                    adapter.removePhotoById(event.photo.id)
                    adapter.addTakenPhoto(event.photo.also { it.photoState = PhotoState.FAILED_TO_UPLOAD })
                    (requireActivity() as PhotosActivity).showKnownErrorMessage(event.errorCode)
                }
                is UploadedPhotosFragmentEvent.PhotoUploadEvent.PhotoAnswerFound -> {
                    adapter.updateUploadedPhotoSetReceiverInfo(event.takenPhotoName)
                }
                is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnEnd -> {
                    startLoadingFirstPageOfPhotos()
                }
                is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnKnownError -> {
                    handleKnownErrors(event.errorCode)
                }
                is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnUnknownError -> {
                    adapter.updateAllPhotosState(PhotoState.FAILED_TO_UPLOAD)
                    handleUnknownErrors(event.error)
                }
            }.safe
        }
    }

    private fun addUploadedPhotosToAdapter(uploadedPhotos: List<UploadedPhoto>) {
        if (!isAdded) {
            return
        }

        uploadedPhotosList.post {
            if (uploadedPhotos.isNotEmpty()) {
                viewModel.uploadedPhotosFragmentViewState.update(newLastId = uploadedPhotos.last().photoId)
                adapter.addUploadedPhotos(uploadedPhotos)
            }

            if (adapter.getUploadedPhotosCount() == 0) {
                showToast(getString(R.string.uploaded_photos_fragment_nothing_found_msg))
            }

            if (uploadedPhotos.size < viewModel.uploadedPhotosFragmentViewState.photosPerPage) {
                endlessScrollListener.stopLoading()
            }

            endlessScrollListener.pageLoaded()
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

    private fun showProgressFooter() {
        if (!isAdded) {
            return
        }

        uploadedPhotosList.post {
            adapter.showProgressFooter()
        }
    }

    private fun hideProgressFooter() {
        if (!isAdded) {
            return
        }

        uploadedPhotosList.post {
            adapter.hideProgressFooter()
        }
    }

    private fun handleKnownErrors(errorCode: ErrorCode) {
        fun handleGetUploadedPhotosErrors(errorCode: ErrorCode.GetUploadedPhotosErrors) {
            hideProgressFooter()
            swipeToRefreshLayout.isRefreshing = false

            if (!isVisible) {
                return
            }

            val message = when (errorCode) {
                is ErrorCode.GetUploadedPhotosErrors.Ok -> null
                is ErrorCode.GetUploadedPhotosErrors.LocalUserIdIsEmpty -> null
                is ErrorCode.GetUploadedPhotosErrors.UnknownError -> "Unknown error"
                is ErrorCode.GetUploadedPhotosErrors.DatabaseError -> "Server database error"
                is ErrorCode.GetUploadedPhotosErrors.BadRequest -> "Bad request error"
                is ErrorCode.GetUploadedPhotosErrors.NoPhotosInRequest -> "Bad request error (no photos in request)"
                is ErrorCode.GetUploadedPhotosErrors.LocalBadServerResponse -> "Bad server response error"
                is ErrorCode.GetUploadedPhotosErrors.LocalTimeout -> "Operation timeout error"
            }

            if (message != null) {
                showToast(message)
            }
        }
        fun handleUploadPhotoErrors(errorCode: ErrorCode.UploadPhotoErrors) {
            (requireActivity() as PhotosActivity).showKnownErrorMessage(errorCode)
            adapter.updateAllPhotosState(PhotoState.FAILED_TO_UPLOAD)
        }

        when (errorCode) {
            is ErrorCode.GetUploadedPhotosErrors -> handleGetUploadedPhotosErrors(errorCode)
            is ErrorCode.UploadPhotoErrors -> handleUploadPhotoErrors(errorCode)
        }
    }

    private fun handleUnknownErrors(error: Throwable) {
        (requireActivity() as PhotosActivity).showUnknownErrorMessage(error)
        hideProgressFooter()
        swipeToRefreshLayout.isRefreshing = false
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
