package com.kirakishou.photoexchange.ui.fragment


import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.kirakishou.photoexchange.mvp.model.exception.ApiErrorException
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.model.other.Constants.DEFAULT_ADAPTER_ITEM_WIDTH
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.adapter.GalleryPhotosAdapter
import com.kirakishou.photoexchange.ui.adapter.GalleryPhotosAdapterSpanSizeLookup
import com.kirakishou.photoexchange.ui.widget.EndlessRecyclerOnScrollListener
import core.ErrorCode
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.consumeEach
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class GalleryFragment : BaseFragment(), StateEventListener<GalleryFragmentEvent>, IntercomListener {

  @BindView(R.id.gallery_photos_list)
  lateinit var galleryPhotosList: RecyclerView

  @Inject
  lateinit var imageLoader: ImageLoader

  @Inject
  lateinit var viewModel: PhotosActivityViewModel

  lateinit var adapter: GalleryPhotosAdapter
  lateinit var endlessScrollListener: EndlessRecyclerOnScrollListener


  private val TAG = "GalleryFragment"
  private val GALLERY_PHOTO_ADAPTER_VIEW_WIDTH = DEFAULT_ADAPTER_ITEM_WIDTH

  private val loadMoreSubject = PublishSubject.create<Unit>()
  private val scrollSubject = PublishSubject.create<Boolean>()
  private val adapterButtonClickSubject = PublishSubject.create<GalleryPhotosAdapter.GalleryPhotosAdapterButtonClickEvent>()

  override fun getContentView(): Int = R.layout.fragment_gallery

  override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
    viewModel.galleryFragmentViewModel.viewState.reset()

    launch { initRx() }
    initRecyclerView()
  }

  override fun onFragmentViewDestroy() {
  }

  private suspend fun initRx() {
    compositeDisposable += viewModel.galleryFragmentViewModel.knownErrors
      .subscribe({ errorCode -> handleKnownErrors(errorCode) })

    compositeDisposable += viewModel.galleryFragmentViewModel.unknownErrors
      .subscribe({ error -> handleUnknownErrors(error) })

    compositeDisposable += loadMoreSubject
      .subscribe({ viewModel.galleryFragmentViewModel.loadMorePhotos() })

    compositeDisposable += scrollSubject
      .subscribeOn(Schedulers.io())
      .distinctUntilChanged()
      .throttleFirst(200, TimeUnit.MILLISECONDS)
      .subscribe({ isScrollingDown ->
        viewModel.intercom.tell<PhotosActivity>()
          .that(PhotosActivityEvent.ScrollEvent(isScrollingDown))
      })

    viewModel.intercom.galleryFragmentEvents.listen().consumeEach { event ->
      onStateEvent(event)
    }

    adapterButtonClickSubject.consumeEach { buttonClickEvent ->
      when (buttonClickEvent) {
        is GalleryPhotosAdapter.GalleryPhotosAdapterButtonClickEvent.FavouriteClicked -> {
          val result = viewModel.favouritePhoto(buttonClickEvent.photoName)

          //TODO: hide inside viewmodel
          when (result) {
            is Either.Value -> favouritePhoto(buttonClickEvent.photoName, result.value.isFavourited, result.value.favouritesCount)
            is Either.Error -> {
              when (result.error) {
                is ApiErrorException -> handleKnownErrors(result.error.errorCode)
                else -> handleUnknownErrors(result.error)
              }
            }
          }
        }
        is GalleryPhotosAdapter.GalleryPhotosAdapterButtonClickEvent.ReportClicked -> {
          val result = viewModel.reportPhoto(buttonClickEvent.photoName)

          //TODO: hide inside viewmodel
          when (result) {
            is Either.Value -> reportPhoto(buttonClickEvent.photoName, result.value)
            is Either.Error -> {
              when (result.error) {
                is ApiErrorException -> handleKnownErrors(result.error.errorCode)
                else -> handleUnknownErrors(result.error)
              }
            }
          }
        }
        is GalleryPhotosAdapter.GalleryPhotosAdapterButtonClickEvent.SwitchShowMapOrPhoto -> {
          handleAdapterClick(buttonClickEvent)
        }
      }.safe
    }
  }

  private fun initRecyclerView() {
    val columnsCount = AndroidUtils.calculateNoOfColumns(requireContext(), GALLERY_PHOTO_ADAPTER_VIEW_WIDTH)

    adapter = GalleryPhotosAdapter(requireContext(), imageLoader, adapterButtonClickSubject)

    val layoutManager = GridLayoutManager(requireContext(), columnsCount)
    layoutManager.spanSizeLookup = GalleryPhotosAdapterSpanSizeLookup(adapter, columnsCount)

    viewModel.galleryFragmentViewModel.viewState.updateCount(Constants.DEFAULT_PHOTOS_PER_PAGE_COUNT * layoutManager.spanCount)

    //TODO: visible threshold should be less than photosPerPage count
    endlessScrollListener = EndlessRecyclerOnScrollListener(TAG, layoutManager, 2, loadMoreSubject, scrollSubject)

    galleryPhotosList.layoutManager = layoutManager
    galleryPhotosList.adapter = adapter
    galleryPhotosList.clearOnScrollListeners()
    galleryPhotosList.addOnScrollListener(endlessScrollListener)
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

  override suspend fun onStateEvent(event: GalleryFragmentEvent) {
    if (!isAdded) {
      return
    }

    when (event) {
      is GalleryFragmentEvent.GeneralEvents -> {
        onUiEvent(event)
      }
    }.safe
  }

  private fun onUiEvent(event: GalleryFragmentEvent.GeneralEvents) {
    if (!isAdded) {
      return
    }

    galleryPhotosList.post {
      when (event) {
        is GalleryFragmentEvent.GeneralEvents.ShowProgressFooter -> {
          showProgressFooter()
        }
        is GalleryFragmentEvent.GeneralEvents.HideProgressFooter -> {
          hideProgressFooter()
        }
        is GalleryFragmentEvent.GeneralEvents.OnPageSelected -> {
          viewModel.galleryFragmentViewModel.viewState.reset()
        }
        is GalleryFragmentEvent.GeneralEvents.PageIsLoading -> {
        }
        is GalleryFragmentEvent.GeneralEvents.ShowGalleryPhotos -> {
          addPhotosToAdapter(event.photos)
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
        viewModel.galleryFragmentViewModel.viewState.updateFromGalleryPhotos(galleryPhotos)
        adapter.addAll(galleryPhotos)
      }

      endlessScrollListener.pageLoaded()

      if (adapter.itemCount == 0) {
        adapter.showMessageFooter("You have not received any photos yet")
        return@post
      }

      if (galleryPhotos.size < viewModel.galleryFragmentViewModel.viewState.count) {
        endlessScrollListener.reachedEnd()
        adapter.showMessageFooter("End of the list reached")
      }
    }
  }

  private fun handleKnownErrors(errorCode: ErrorCode) {
    hideProgressFooter()
    (activity as? PhotosActivity)?.showKnownErrorMessage(errorCode)
  }

  private fun handleUnknownErrors(error: Throwable) {
    Timber.tag(TAG).e(error)

    hideProgressFooter()
    (activity as? PhotosActivity)?.showUnknownErrorMessage(error)
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
