package com.kirakishou.photoexchange.ui.fragment

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.Toast
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.RxLifecycle
import com.kirakishou.photoexchange.helper.intercom.StateEventListener
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.adapter.UploadedPhotosAdapter
import com.kirakishou.photoexchange.ui.adapter.UploadedPhotosAdapterSpanSizeLookup
import com.kirakishou.photoexchange.ui.viewstate.UploadedPhotosFragmentViewState
import com.kirakishou.photoexchange.ui.widget.EndlessRecyclerOnScrollListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit
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
    private val onPhotosUploadedSubject = BehaviorSubject.createDefault(false).toSerialized()
    private var viewState = UploadedPhotosFragmentViewState()
    private val loadMoreSubject = PublishSubject.create<Int>()
    private var photosPerPage = 0
    private var lastId = Long.MAX_VALUE

    override fun getContentView(): Int = R.layout.fragment_uploaded_photos

    //TODO fix photos not loading when changing from this fragment to gallery fragment and then back
    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
        initRx()
        initRecyclerView()

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

        val layoutManager = GridLayoutManager(requireContext(), columnsCount)
        layoutManager.spanSizeLookup = UploadedPhotosAdapterSpanSizeLookup(adapter, columnsCount)
        photosPerPage = Constants.UPLOADED_PHOTOS_PER_ROW * layoutManager.spanCount

        endlessScrollListener = EndlessRecyclerOnScrollListener(TAG, layoutManager, photosPerPage, loadMoreSubject, 0)

        uploadedPhotosList.layoutManager = layoutManager
        uploadedPhotosList.adapter = adapter
        uploadedPhotosList.clearOnScrollListeners()
        uploadedPhotosList.addOnScrollListener(endlessScrollListener)
    }

    private fun initRx() {
        compositeDisposable += viewModel.eventForwarder.getUploadedPhotosFragmentEventsStream()
            .observeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { viewState -> onStateEvent(viewState) }
            .doOnError { Timber.tag(TAG).e(it) }
            .subscribe()

        compositeDisposable += failedToUploadPhotoButtonClicksSubject
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { Timber.tag(TAG).e(it) }
            .subscribe({ viewModel.eventForwarder.sendPhotoActivityEvent(PhotosActivityEvent.FailedToUploadPhotoButtonClick(it)) })

        compositeDisposable += lifecycle.getLifecycle()
            .filter { lifecycle -> lifecycle.isAtLeast(RxLifecycle.FragmentState.Resumed) }
            .concatMap { viewModel.checkHasPhotosToUpload() }
            .filter { hasPhotosToUpload -> !hasPhotosToUpload }
            .subscribe({
                photosUploaded()
                loadFirstPageOfUploadedPhotos()
            })

        compositeDisposable += lifecycle.getLifecycle()
            .filter { lifecycle -> lifecycle.isAtLeast(RxLifecycle.FragmentState.Resumed) }
            .concatMap { viewModel.checkHasPhotosToReceive() }
            .filter { hasPhotosToReceive -> hasPhotosToReceive }
            .concatMap { viewModel.checkHasPhotosToUpload() }
            .filter { hasPhotosToUpload -> !hasPhotosToUpload }
            .subscribe({
                viewModel.eventForwarder.sendPhotoActivityEvent(
                    PhotosActivityEvent.StartReceivingService(
                        UploadedPhotosFragment::class.java,
                        "Starting the service after onResume event when there photos to receive and no photos to upload"
                    ))
            })

        compositeDisposable += Observables.combineLatest(loadMoreSubject, onPhotosUploadedSubject)
            .filter { it.second }
            .map { it.first }
            .concatMap { nextPage ->
                return@concatMap Observable.just(1)
                    .doOnNext { onUiEvent(UploadedPhotosFragmentEvent.UiEvents.ShowProgressFooter()) }
                    .concatMap { viewModel.loadNextPageOfUploadedPhotos(lastId, photosPerPage) }
                    .doOnNext { onUiEvent(UploadedPhotosFragmentEvent.UiEvents.HideProgressFooter()) }
                    //add slight delay to ensure progressbar is removed from recyclerview before adding other elements
                    //otherwise it will scroll the recyclerview to the bottom
                    .delay(500, TimeUnit.MILLISECONDS)
            }
            .subscribe({ result ->
                when (result) {
                    is Either.Value -> addUploadedPhotosToAdapter(result.value)
                    is Either.Error -> handleError(result.error)
                }
            }, { error ->
                Timber.tag(TAG).e(error)
                onUiEvent(UploadedPhotosFragmentEvent.UiEvents.HideProgressFooter())
            })
    }

    private fun loadTakenPhotos() {
        compositeDisposable += viewModel.loadMyPhotos()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { photos -> addTakenPhotosToAdapter(photos) }
            .flatMapObservable { _ ->
                return@flatMapObservable viewModel.checkHasPhotosToUpload()
                    .doOnNext { hasPhotosToUpload ->
                        if (hasPhotosToUpload) {
                            viewModel.eventForwarder.sendPhotoActivityEvent(PhotosActivityEvent
                                .StartUploadingService(UploadedPhotosFragment::class.java,
                                    "Starting the service after taken photos were loaded")
                            )
                        }
                    }
            }
            .subscribe()
    }

    private fun loadFirstPageOfUploadedPhotos() {
        loadMoreSubject.onNext(0)
    }

    private fun photosUploaded() {
        onPhotosUploadedSubject.onNext(true)
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
        uploadedPhotosList.post {
            when (event) {
                is UploadedPhotosFragmentEvent.UiEvents.ScrollToTop -> {
                    uploadedPhotosList.scrollToPosition(0)
                }
                is UploadedPhotosFragmentEvent.UiEvents.ShowProgressFooter -> {
                    addProgressFooter()
                }
                is UploadedPhotosFragmentEvent.UiEvents.HideProgressFooter -> {
                    hideProgressFooter()
                }
                is UploadedPhotosFragmentEvent.UiEvents.RemovePhoto -> {
                    adapter.removePhotoById(event.photo.id)
                }
                is UploadedPhotosFragmentEvent.UiEvents.AddPhoto -> {
                    adapter.addTakenPhoto(event.photo)
                }
                is UploadedPhotosFragmentEvent.UiEvents.OnPhotoRemoved -> {
                    if (adapter.getFailedPhotosCount() == 0) {
                        loadFirstPageOfUploadedPhotos()
                    }
                }
                is UploadedPhotosFragmentEvent.UiEvents.LoadTakenPhotos -> {
                    loadTakenPhotos()
                }
                else -> throw IllegalArgumentException("Unknown UploadedPhotosFragmentEvent $event")
            }
        }
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
                is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnFoundPhotoAnswer -> {
                    adapter.updateUploadedPhotoSetReceiverInfo(event.takenPhotoName)
                }
                is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnEnd -> {
                    photosUploaded()
                }
                is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnError -> {
                    when (event.error) {
                        is UploadedPhotosFragmentEvent.UploadingError.KnownError -> {
                            handleKnownErrors(event.error.errorCode)
                        }
                        is UploadedPhotosFragmentEvent.UploadingError.UnknownError -> {
                            handleUnknownErrors(event.error.error)
                        }
                    }
                }
                else -> throw IllegalArgumentException("Unknown PhotoUploadEvent $event")
            }
        }
    }

    private fun handleKnownErrors(errorCode: ErrorCode.UploadPhotoErrors) {
        (requireActivity() as PhotosActivity).showKnownErrorMessage(errorCode)
        adapter.updateAllPhotosState(PhotoState.FAILED_TO_UPLOAD)
    }

    private fun handleUnknownErrors(error: Throwable) {
        (requireActivity() as PhotosActivity).showUnknownErrorMessage(error)
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

            if (adapter.getUploadedPhotosCount() == 0) {
                showToast(getString(R.string.uploaded_photos_fragment_nothing_found_msg))
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

    private fun hideProgressFooter() {
        uploadedPhotosList.post {
            adapter.hideProgressFooter()
        }
    }

    private fun handleError(errorCode: ErrorCode.GetUploadedPhotosErrors) {
        hideProgressFooter()

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
