package com.kirakishou.photoexchange.ui.fragment


import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import com.airbnb.epoxy.AsyncEpoxyController
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.IntercomListener
import com.kirakishou.photoexchange.helper.intercom.StateEventListener
import com.kirakishou.photoexchange.helper.intercom.event.GalleryFragmentEvent
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.helper.Constants
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class GalleryFragment : BaseMvRxFragment(), StateEventListener<GalleryFragmentEvent>, IntercomListener {

  @BindView(R.id.gallery_photos_list)
  lateinit var galleryPhotosList: RecyclerView

  @Inject
  lateinit var imageLoader: ImageLoader

  @Inject
  lateinit var viewModel: PhotosActivityViewModel

  private val TAG = "GalleryFragment"

  private val galleryPhotoAdapterViewWidth = Constants.DEFAULT_ADAPTER_ITEM_WIDTH

  private val photoSize by lazy { AndroidUtils.figureOutPhotosSizes(requireContext()) }
  private val columnsCount by lazy { AndroidUtils.calculateNoOfColumns(requireContext(), galleryPhotoAdapterViewWidth) }

  private val scrollSubject = PublishSubject.create<Boolean>()

  override fun getFragmentLayoutId(): Int = R.layout.fragment_mvrx

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    viewModel.galleryFragmentViewModel.photoSize = photoSize
    viewModel.galleryFragmentViewModel.photosPerPage = columnsCount * Constants.DEFAULT_PHOTOS_PER_PAGE_COUNT

    viewModel.galleryFragmentViewModel.subscribe(this, true) {
      doInvalidate()
    }

    swipeRefreshLayout.setOnRefreshListener {
      swipeRefreshLayout.isRefreshing = false
      viewModel.galleryFragmentViewModel.resetState()
    }

    launch { initRx() }
  }

  private suspend fun initRx() {
    compositeDisposable += scrollSubject
      .subscribeOn(Schedulers.io())
      .distinctUntilChanged()
      .throttleFirst(200, TimeUnit.MILLISECONDS)
      .subscribe({ isScrollingDown ->
        viewModel.intercom.tell<PhotosActivity>()
          .that(PhotosActivityEvent.ScrollEvent(isScrollingDown))
      })

    compositeDisposable += viewModel.intercom.galleryFragmentEvents.listen()
      .subscribe({ event ->
        launch { onStateEvent(event) }
      })

    TODO()
//    compositeDisposable += adapterButtonClickSubject
//      .subscribe({ buttonClickEvent ->
//
//        when (buttonClickEvent) {
//          is GalleryPhotosAdapter.GalleryPhotosAdapterButtonClickEvent.FavouriteClicked -> {
//            val result = viewModel.favouritePhoto(buttonClickEvent.photoName)
//
//            //TODO: hide inside viewmodel
//            when (result) {
//              is Either.Value -> favouritePhoto(buttonClickEvent.photoName, result.value.isFavourited, result.value.favouritesCount)
//              is Either.Error -> {
//                when (result.error) {
//                  is ApiErrorException -> handleKnownErrors(result.error.errorCode)
//                  else -> handleUnknownErrors(result.error)
//                }
//              }
//            }
//          }
//          is GalleryPhotosAdapter.GalleryPhotosAdapterButtonClickEvent.ReportClicked -> {
//            val result = viewModel.reportPhoto(buttonClickEvent.photoName)
//
//            //TODO: hide inside viewmodel
//            when (result) {
//              is Either.Value -> reportPhoto(buttonClickEvent.photoName, result.value)
//              is Either.Error -> {
//                when (result.error) {
//                  is ApiErrorException -> handleKnownErrors(result.error.errorCode)
//                  else -> handleUnknownErrors(result.error)
//                }
//              }
//            }
//          }
//          is GalleryPhotosAdapter.GalleryPhotosAdapterButtonClickEvent.SwitchShowMapOrPhoto -> {
//            handleAdapterClick(buttonClickEvent)
//          }
//        }.safe
//      })
  }

  override fun buildEpoxyController(): AsyncEpoxyController = simpleController {
    TODO()
  }

  override suspend fun onStateEvent(event: GalleryFragmentEvent) {
    if (!isAdded) {
      return
    }

    TODO()
//    when (event) {
//      is GalleryFragmentEvent.GeneralEvents -> {
//        onUiEvent(event)
//      }
//    }.safe
  }

//  private fun onUiEvent(event: GalleryFragmentEvent.GeneralEvents) {
//    if (!isAdded) {
//      return
//    }
//  }

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
