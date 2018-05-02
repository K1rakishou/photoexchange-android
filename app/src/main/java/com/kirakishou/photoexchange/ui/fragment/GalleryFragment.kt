package com.kirakishou.photoexchange.ui.fragment


import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.Toast
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.model.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.viewmodel.AllPhotosActivityViewModel
import com.kirakishou.photoexchange.ui.activity.AllPhotosActivity
import com.kirakishou.photoexchange.ui.adapter.GalleryPhotosAdapter
import com.kirakishou.photoexchange.ui.adapter.GalleryPhotosAdapterSpanSizeLookup
import com.kirakishou.photoexchange.ui.widget.EndlessRecyclerOnScrollListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class GalleryFragment : BaseFragment() {

    @BindView(R.id.gallery_photos_list)
    lateinit var galleryPhotosList: RecyclerView

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var viewModel: AllPhotosActivityViewModel

    private val _tag = "GalleryFragment"
    private val GALLERY_PHOTO_ADAPTER_VIEW_WIDTH = 288
    private val loadMoreSubject = PublishSubject.create<Int>()
    private val adapterButtonClickSubject = PublishSubject.create<GalleryPhotosAdapter.GalleryPhotosAdapterButtonClickEvent>()
    private var photosPerPage = 0
    private var lastId = Long.MAX_VALUE

    lateinit var adapter: GalleryPhotosAdapter
    lateinit var endlessScrollListener: EndlessRecyclerOnScrollListener

    override fun getContentView(): Int = R.layout.fragment_gallery

    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
        initRx()
        initRecyclerView()
        loadFirstPage()
    }

    override fun onFragmentViewDestroy() {
        adapter.cleanUp()
    }

    private fun loadFirstPage() {
        compositeDisposable += viewModel.loadNextPageOfGalleryPhotos(lastId, photosPerPage)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { photos -> addPhotoToAdapter(photos) }
            .subscribe()
    }

    private fun initRx() {
        compositeDisposable += loadMoreSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .doOnNext { addProgressFooter() }
            .concatMap { viewModel.loadNextPageOfGalleryPhotos(lastId, photosPerPage) }
            .delay(2, TimeUnit.SECONDS, Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { removeProgressFooter() }
            .doOnNext { photos -> addPhotoToAdapter(photos) }
            .subscribe()

        compositeDisposable += adapterButtonClickSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .filter { buttonClicked -> buttonClicked is GalleryPhotosAdapter.GalleryPhotosAdapterButtonClickEvent.FavouriteClicked }
            .cast(GalleryPhotosAdapter.GalleryPhotosAdapterButtonClickEvent.FavouriteClicked::class.java)
            .concatMap { viewModel.favouritePhoto(it.photoName).zipWith(Observable.just(it.photoName)) }
            .doOnNext { (response, photoName) -> favouritePhoto(photoName, response.first, response.second) }
            .subscribe()

        compositeDisposable += adapterButtonClickSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .filter { buttonClicked -> buttonClicked is GalleryPhotosAdapter.GalleryPhotosAdapterButtonClickEvent.ReportClicked }
            .cast(GalleryPhotosAdapter.GalleryPhotosAdapterButtonClickEvent.ReportClicked::class.java)
            .concatMap { viewModel.reportPhoto(it.photoName).zipWith(Observable.just(it.photoName)) }
            .doOnNext { (isReported, photoName) -> reportPhoto(photoName, isReported) }
            .subscribe()
    }

    private fun initRecyclerView() {
        val columnsCount = AndroidUtils.calculateNoOfColumns(requireContext(), GALLERY_PHOTO_ADAPTER_VIEW_WIDTH)

        adapter = GalleryPhotosAdapter(requireContext(), imageLoader, columnsCount, adapterButtonClickSubject)
        adapter.init()

        val layoutManager = GridLayoutManager(requireContext(), columnsCount)
        layoutManager.spanSizeLookup = GalleryPhotosAdapterSpanSizeLookup(adapter, columnsCount)
        photosPerPage = Constants.GALLERY_PHOTOS_PER_ROW * layoutManager.spanCount

        endlessScrollListener = EndlessRecyclerOnScrollListener(layoutManager, photosPerPage, loadMoreSubject)

        galleryPhotosList.layoutManager = layoutManager
        galleryPhotosList.adapter = adapter
        galleryPhotosList.clearOnScrollListeners()
        galleryPhotosList.addOnScrollListener(endlessScrollListener)
    }

    private fun favouritePhoto(photoName: String, isFavourited: Boolean, favouritesCount: Long) {
        galleryPhotosList.post {
            if (!adapter.favouritePhoto(photoName, isFavourited, favouritesCount)) {
                return@post
            }

            if (isFavourited) {
                (requireActivity() as AllPhotosActivity).showToast(getString(R.string.photo_favourited_text), Toast.LENGTH_SHORT)
            } else {
                (requireActivity() as AllPhotosActivity).showToast(getString(R.string.photo_unfavourited_text), Toast.LENGTH_SHORT)
            }
        }
    }

    private fun reportPhoto(photoName: String, isReported: Boolean) {
        galleryPhotosList.post {
            if (!adapter.reportPhoto(photoName, isReported)) {
                return@post
            }

            if (isReported) {
                (requireActivity() as AllPhotosActivity).showToast(getString(R.string.photo_reported_text), Toast.LENGTH_SHORT)
            } else {
                (requireActivity() as AllPhotosActivity).showToast(getString(R.string.photo_unreported_text), Toast.LENGTH_SHORT)
            }
        }
    }

    private fun addProgressFooter() {
        galleryPhotosList.post {
            adapter.addProgressFooter()
        }
    }

    private fun removeProgressFooter() {
        galleryPhotosList.post {
            adapter.removeProgressFooter()
        }
    }

    private fun addPhotoToAdapter(photos: List<GalleryPhoto>) {
        galleryPhotosList.post {
            endlessScrollListener.pageLoaded()

            if (photos.isNotEmpty()) {
                lastId = photos.last().remoteId
            }

            if (photos.size < photosPerPage) {
                endlessScrollListener.reachedEnd()
            }

            adapter.addAll(photos)
        }
    }

    override fun resolveDaggerDependency() {
        (requireActivity() as AllPhotosActivity).activityComponent
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
