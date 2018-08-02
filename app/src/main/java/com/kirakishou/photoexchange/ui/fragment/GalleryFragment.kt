package com.kirakishou.photoexchange.ui.fragment


import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.Toast
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.IntercomListener
import com.kirakishou.photoexchange.helper.intercom.StateEventListener
import com.kirakishou.photoexchange.helper.intercom.event.GalleryFragmentEvent
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.model.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.model.other.Constants.DEFAULT_ADAPTER_ITEM_WIDTH
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.adapter.GalleryPhotosAdapter
import com.kirakishou.photoexchange.ui.adapter.GalleryPhotosAdapterSpanSizeLookup
import com.kirakishou.photoexchange.ui.viewstate.GalleryFragmentViewState
import com.kirakishou.photoexchange.ui.widget.EndlessRecyclerOnScrollListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject

class GalleryFragment : BaseFragment(), StateEventListener<GalleryFragmentEvent>, IntercomListener {

    @BindView(R.id.gallery_photos_list)
    lateinit var galleryPhotosList: RecyclerView

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var viewModel: PhotosActivityViewModel

    private val TAG = "GalleryFragment"
    private val GALLERY_PHOTO_ADAPTER_VIEW_WIDTH = DEFAULT_ADAPTER_ITEM_WIDTH
    private val loadMoreSubject = PublishSubject.create<Int>()
    private val scrollSubject = PublishSubject.create<Boolean>()
    private val adapterButtonClickSubject = PublishSubject.create<GalleryPhotosAdapter.GalleryPhotosAdapterButtonClickEvent>()
    private val viewState = GalleryFragmentViewState()
    private var photosPerPage = 0

    lateinit var adapter: GalleryPhotosAdapter
    lateinit var endlessScrollListener: EndlessRecyclerOnScrollListener

    override fun getContentView(): Int = R.layout.fragment_gallery

    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
        initRx()
        initRecyclerView()
        loadFirstPage()
    }

    override fun onFragmentViewDestroy() {
    }

    private fun initRx() {
        compositeDisposable += viewModel.intercom.galleryFragmentEvents.listen()
            .doOnNext { viewState -> onStateEvent(viewState) }
            .subscribe({ }, { Timber.tag(TAG).e(it) })

        compositeDisposable += loadMoreSubject
            .doOnNext { endlessScrollListener.pageLoading() }
            .concatMap {
                return@concatMap viewModel.loadNextPageOfGalleryPhotos(viewState.lastId, photosPerPage)
                    .flatMap(this::preloadPhotos)
            }
            .subscribe({ result ->
                when (result) {
                    is Either.Value -> addPhotosToAdapter(result.value)
                    is Either.Error -> handleGetGalleryPhotosError(result.error)
                }
            }, { Timber.tag(TAG).e(it) })

        compositeDisposable += adapterButtonClickSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .filter { buttonClicked -> buttonClicked is GalleryPhotosAdapter.GalleryPhotosAdapterButtonClickEvent.FavouriteClicked }
            .cast(GalleryPhotosAdapter.GalleryPhotosAdapterButtonClickEvent.FavouriteClicked::class.java)
            .concatMap { viewModel.favouritePhoto(it.photoName).zipWith(Observable.just(it.photoName)) }
            .subscribe({ resultPair ->
                val result = resultPair.first
                val photoName = resultPair.second

                when (result) {
                    is Either.Value -> favouritePhoto(photoName, result.value.isFavourited, result.value.favouritesCount)
                    is Either.Error -> handleFavouritePhotoError(result.error)
                }
            }, { Timber.tag(TAG).e(it) })

        compositeDisposable += adapterButtonClickSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .filter { buttonClicked -> buttonClicked is GalleryPhotosAdapter.GalleryPhotosAdapterButtonClickEvent.ReportClicked }
            .cast(GalleryPhotosAdapter.GalleryPhotosAdapterButtonClickEvent.ReportClicked::class.java)
            .concatMap { viewModel.reportPhoto(it.photoName).zipWith(Observable.just(it.photoName)) }
            .subscribe({ resultPair ->
                val result = resultPair.first
                val photoName = resultPair.second

                when (result) {
                    is Either.Value -> reportPhoto(photoName, result.value)
                    is Either.Error -> handleReportPhotoError(result.error)
                }
            }, { Timber.tag(TAG).e(it) })

        compositeDisposable += adapterButtonClickSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .filter { buttonClicked -> buttonClicked is GalleryPhotosAdapter.GalleryPhotosAdapterButtonClickEvent.SwitchShowMapOrPhoto }
            .cast(GalleryPhotosAdapter.GalleryPhotosAdapterButtonClickEvent.SwitchShowMapOrPhoto::class.java)
            .subscribe({ click -> handleAdapterClick(click) }, { Timber.tag(TAG).e(it) })

        compositeDisposable += scrollSubject
            .distinctUntilChanged()
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribe({ isScrollingDown ->
                viewModel.intercom.tell<PhotosActivity>()
                    .that(PhotosActivityEvent.ScrollEvent(isScrollingDown))
            })
    }

    private fun initRecyclerView() {
        val columnsCount = AndroidUtils.calculateNoOfColumns(requireContext(), GALLERY_PHOTO_ADAPTER_VIEW_WIDTH)

        adapter = GalleryPhotosAdapter(requireContext(), imageLoader, adapterButtonClickSubject)

        val layoutManager = GridLayoutManager(requireContext(), columnsCount)
        layoutManager.spanSizeLookup = GalleryPhotosAdapterSpanSizeLookup(adapter, columnsCount)

        photosPerPage = Constants.GALLERY_PHOTOS_PER_ROW * layoutManager.spanCount
        endlessScrollListener = EndlessRecyclerOnScrollListener(TAG, layoutManager, photosPerPage, loadMoreSubject, scrollSubject, 1)

        galleryPhotosList.layoutManager = layoutManager
        galleryPhotosList.adapter = adapter
        galleryPhotosList.clearOnScrollListeners()
        galleryPhotosList.addOnScrollListener(endlessScrollListener)
    }

    private fun loadFirstPage() {
        loadMoreSubject.onNext(0)
    }

    private fun handleAdapterClick(click: GalleryPhotosAdapter.GalleryPhotosAdapterButtonClickEvent) {
        if (!isAdded) {
            return
        }

        when (click) {
            is GalleryPhotosAdapter.GalleryPhotosAdapterButtonClickEvent.SwitchShowMapOrPhoto -> {
                switchShowMapOrPhoto(click.photoName)
            }
            is GalleryPhotosAdapter.GalleryPhotosAdapterButtonClickEvent.FavouriteClicked,
            is GalleryPhotosAdapter.GalleryPhotosAdapterButtonClickEvent.ReportClicked -> {
                throw IllegalStateException("Should not happen")
            }
        }.safe
    }

    private fun switchShowMapOrPhoto(photoName: String) {
        if (!isAdded) {
            return
        }

        galleryPhotosList.post {
            adapter.switchShowMapOrPhoto(photoName)
        }
    }

    private fun preloadPhotos(
        result: Either<ErrorCode.GetGalleryPhotosErrors, List<GalleryPhoto>>
    ): Observable<Either<ErrorCode.GetGalleryPhotosErrors, List<GalleryPhoto>>> {
        if (result is Either.Error) {
            return Observable.just(result)
        }

        return Observable.fromIterable((result as Either.Value).value)
            .subscribeOn(Schedulers.io())
            .flatMapSingle { galleryPhoto ->
                return@flatMapSingle imageLoader.preloadImageFromNetAsync(galleryPhoto.photoName)
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .doOnSuccess { result ->
                        if (!result) {
                            Timber.tag(TAG).w("Could not pre-load photo ${galleryPhoto.photoName}")
                        }
                    }
            }
            .toList()
            .toObservable()
            .map { result }
    }

    private fun favouritePhoto(photoName: String, isFavourited: Boolean, favouritesCount: Long) {
        if (!isAdded) {
            return
        }

        galleryPhotosList.post {
            adapter.favouritePhoto(photoName, isFavourited, favouritesCount)
        }
    }

    private fun reportPhoto(photoName: String, isReported: Boolean) {
        if (!isAdded) {
            return
        }

        galleryPhotosList.post {
            if (!adapter.reportPhoto(photoName, isReported)) {
                return@post
            }

            if (isReported) {
                (requireActivity() as PhotosActivity).showToast(getString(R.string.photo_reported_text), Toast.LENGTH_SHORT)
            } else {
                (requireActivity() as PhotosActivity).showToast(getString(R.string.photo_unreported_text), Toast.LENGTH_SHORT)
            }
        }
    }

    override fun onStateEvent(event: GalleryFragmentEvent) {
        if (!isAdded) {
            return
        }

        requireActivity().runOnUiThread {
            when (event) {
                is GalleryFragmentEvent.UiEvents -> {
                    onUiEvent(event)
                }
            }.safe
        }
    }

    private fun onUiEvent(event: GalleryFragmentEvent.UiEvents) {
        if (!isAdded) {
            return
        }

        galleryPhotosList.post {
            when (event) {
                is GalleryFragmentEvent.UiEvents.ShowProgressFooter -> {
                    showProgressFooter()
                }
                is GalleryFragmentEvent.UiEvents.HideProgressFooter -> {
                    hideProgressFooter()
                }
            }.safe
        }
    }

    private fun showProgressFooter() {
        if (!isAdded) {
            return
        }

        galleryPhotosList.post {
            adapter.showProgressFooter()
        }
    }

    private fun hideProgressFooter() {
        if (!isAdded) {
            return
        }

        galleryPhotosList.post {
            adapter.clearFooter()
        }
    }

    private fun addPhotosToAdapter(galleryPhotos: List<GalleryPhoto>) {
        if (!isAdded) {
            return
        }

        galleryPhotosList.post {
            if (galleryPhotos.isNotEmpty()) {
                viewState.updateLastId(galleryPhotos.last().galleryPhotoId)
                adapter.addAll(galleryPhotos)
            }

            endlessScrollListener.pageLoaded()

            if (adapter.itemCount == 0) {
                adapter.showMessageFooter("You have not received any photos yet")
                return@post
            }

            if (galleryPhotos.size < photosPerPage) {
                endlessScrollListener.reachedEnd()
                adapter.showMessageFooter("Bottom of the list reached")
            }
        }
    }

    private fun handleReportPhotoError(errorCode: ErrorCode.ReportPhotoErrors) {
        if (!isVisible) {
            return
        }

        val message = when (errorCode) {
            is ErrorCode.ReportPhotoErrors.Ok -> null
            is ErrorCode.ReportPhotoErrors.UnknownError -> "Unknown error"
            is ErrorCode.ReportPhotoErrors.BadRequest -> "Bad request error"
            is ErrorCode.ReportPhotoErrors.LocalBadServerResponse -> "Bad server response error"
            is ErrorCode.ReportPhotoErrors.LocalTimeout -> "Operation timeout error"
        }

        if (message != null) {
            showToast(message)
        }
    }

    private fun handleFavouritePhotoError(errorCode: ErrorCode.FavouritePhotoErrors) {
        if (!isVisible) {
            return
        }

        val message = when (errorCode) {
            is ErrorCode.FavouritePhotoErrors.Ok -> null
            is ErrorCode.FavouritePhotoErrors.UnknownError -> "Unknown error"
            is ErrorCode.FavouritePhotoErrors.BadRequest -> "Bad request error"
            is ErrorCode.FavouritePhotoErrors.LocalBadServerResponse -> "Bad server response error"
            is ErrorCode.FavouritePhotoErrors.LocalTimeout -> "Operation timeout error"
        }

        if (message != null) {
            showToast(message)
        }
    }

    private fun handleGetGalleryPhotosError(errorCode: ErrorCode.GetGalleryPhotosErrors) {
        hideProgressFooter()

        if (!isVisible) {
            return
        }

        val message = when (errorCode) {
            is ErrorCode.GetGalleryPhotosErrors.Ok -> null
            is ErrorCode.GetGalleryPhotosErrors.UnknownError -> "Unknown error"
            is ErrorCode.GetGalleryPhotosErrors.BadRequest -> "Bad request error"
            is ErrorCode.GetGalleryPhotosErrors.NoPhotosInRequest -> "Bad request error (no photos in request)"
            is ErrorCode.GetGalleryPhotosErrors.LocalBadServerResponse -> "Bad server response error"
            is ErrorCode.GetGalleryPhotosErrors.LocalTimeout -> "Operation timeout error"
            is ErrorCode.GetGalleryPhotosErrors.LocalDatabaseError -> "Operation timeout error"
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
        fun newInstance(): GalleryFragment {
            val fragment = GalleryFragment()
            val args = Bundle()

            fragment.arguments = args
            return fragment
        }
    }
}
