package com.kirakishou.photoexchange.ui.fragment


import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.*
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.IntercomListener
import com.kirakishou.photoexchange.helper.intercom.StateEventListener
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.intercom.event.ReceivedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.adapter.ReceivedPhotosAdapter
import com.kirakishou.photoexchange.ui.adapter.epoxy.loadingRow
import com.kirakishou.photoexchange.ui.adapter.epoxy.receivedPhotoRow
import com.kirakishou.photoexchange.ui.adapter.epoxy.textRow
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.consumeEach
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ReceivedPhotosFragment : BaseMvRxFragment(), StateEventListener<ReceivedPhotosFragmentEvent>, IntercomListener {

  @BindView(R.id.received_photos_list)
  lateinit var receivedPhotosList: RecyclerView

  @Inject
  lateinit var imageLoader: ImageLoader

  @Inject
  lateinit var viewModel: PhotosActivityViewModel

  private val TAG = "ReceivedPhotosFragment"
  private val scrollSubject = PublishSubject.create<Boolean>()
  private val adapterClicksSubject = PublishSubject.create<ReceivedPhotosAdapter.ReceivedPhotosAdapterClickEvent>()

  private val receivedPhotoAdapterViewWidth = Constants.DEFAULT_ADAPTER_ITEM_WIDTH
  private val throttleTime = 200L

  private val photoSize by lazy { AndroidUtils.figureOutPhotosSizes(requireContext()) }
  private val columnsCount by lazy { AndroidUtils.calculateNoOfColumns(requireContext(), receivedPhotoAdapterViewWidth) }

  override fun getFragmentLayoutId(): Int = R.layout.fragment_mvrx

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    viewModel.receivedPhotosFragmentViewModel.photoSize = photoSize
    viewModel.receivedPhotosFragmentViewModel.photosPerPage = columnsCount * Constants.DEFAULT_PHOTOS_PER_PAGE_COUNT

    viewModel.receivedPhotosFragmentViewModel.subscribe(this, true) {
      doInvalidate()
    }

    launch { initRx() }
  }

  private suspend fun initRx() {
    compositeDisposable += adapterClicksSubject
      .subscribeOn(AndroidSchedulers.mainThread())
      .subscribe({ click -> handleAdapterClick(click) }, { Timber.tag(TAG).e(it) })

    compositeDisposable += scrollSubject
      .subscribeOn(Schedulers.io())
      .distinctUntilChanged()
      .throttleFirst(throttleTime, TimeUnit.MILLISECONDS)
      .subscribe({ isScrollingDown ->
        viewModel.intercom.tell<PhotosActivity>()
          .that(PhotosActivityEvent.ScrollEvent(isScrollingDown))
      })

    launch {
      viewModel.intercom.receivedPhotosFragmentEvents.listen().consumeEach { event ->
        onStateEvent(event)
      }
    }
  }

  override fun buildEpoxyController(): AsyncEpoxyController = simpleController {
    return@simpleController withState(viewModel.receivedPhotosFragmentViewModel) { state ->
      when (state.receivedPhotosRequest) {
        is Loading,
        is Success -> {
          if (state.receivedPhotosRequest is Loading) {
            Timber.tag(TAG).d("Loading received photos")

            loadingRow {
              id("received_photos_loading_row")
            }
          } else {
            Timber.tag(TAG).d("Success received photos")
          }

          if (state.receivedPhotos.isEmpty()) {
            textRow {
              id("no_received_photos")
              text("You have no photos yet")
            }
          } else {
            state.receivedPhotos.forEach { photo ->
              receivedPhotoRow {
                id("received_photo_${photo.photoId}")
                photo(photo)
                callback { _ -> viewModel.receivedPhotosFragmentViewModel.swapPhotoAndMap() }
              }
            }

            if (state.isEndReached) {
              textRow {
                id("list_end_footer_text")
                text("End of the list reached.\nClick here to reload")
                callback { _ ->
                  Timber.tag(TAG).d("Reloading")
                  viewModel.receivedPhotosFragmentViewModel.resetState()
                }
              }
            } else {
              loadingRow {
                //we should change the id to trigger the binding
                id("load_next_page_${state.receivedPhotos.size}")
                onBind { _, _, _ -> viewModel.receivedPhotosFragmentViewModel.loadReceivedPhotos() }
              }
            }
          }
        }
        is Fail -> {
          Timber.tag(TAG).d("Fail uploaded photos")

          textRow {
            val exceptionMessage = state.receivedPhotosRequest.error.message ?: "Unknown error message"
            Toast.makeText(requireContext(), "Exception message is: \"$exceptionMessage\"", Toast.LENGTH_LONG).show()

            id("unknown_error")
            text("Unknown error has occurred while trying to load photos from the database. \nClick here to retry")
            callback { _ ->
              Timber.tag(TAG).d("Reloading")
              viewModel.receivedPhotosFragmentViewModel.resetState()
            }
          }
        }
        Uninitialized -> {
          //do nothing
        }
      }.safe
    }
  }

  private fun handleAdapterClick(click: ReceivedPhotosAdapter.ReceivedPhotosAdapterClickEvent) {
    if (!isAdded) {
      return
    }

    when (click) {
      is ReceivedPhotosAdapter.ReceivedPhotosAdapterClickEvent.SwitchShowMapOrPhoto -> {
        switchShowMapOrPhoto(click.photoName)
      }
    }.safe
  }

  override suspend fun onStateEvent(event: ReceivedPhotosFragmentEvent) {

    when (event) {
      is ReceivedPhotosFragmentEvent.GeneralEvents -> {
        kotlin.run {
          if (isAdded) {
            onUiEvent(event)
          }
        }
      }
      is ReceivedPhotosFragmentEvent.ReceivePhotosEvent -> {
        //TODO: move to viewModel
        onReceivePhotosEvent(event)
      }
    }.safe
  }

  private suspend fun onUiEvent(event: ReceivedPhotosFragmentEvent.GeneralEvents) {
    when (event) {
      is ReceivedPhotosFragmentEvent.GeneralEvents.ScrollToTop -> {
        recyclerView.scrollToPosition(0)
      }
      is ReceivedPhotosFragmentEvent.GeneralEvents.OnPageSelected -> {
      }
    }.safe
  }

  private fun onReceivePhotosEvent(event: ReceivedPhotosFragmentEvent.ReceivePhotosEvent) {
    when (event) {
      is ReceivedPhotosFragmentEvent.ReceivePhotosEvent.PhotosReceived -> {
        //TODO:
//          adapter.addReceivedPhoto(event.receivedPhoto)
      }
      is ReceivedPhotosFragmentEvent.ReceivePhotosEvent.OnFailed -> {
        //TODO: do nothing here???
      }
    }.safe
  }

  private fun switchShowMapOrPhoto(photoName: String) {
    if (!isAdded) {
      return
    }

    receivedPhotosList.post {
      //TODO
//      adapter.switchShowMapOrPhoto(photoName)
    }
  }

  override fun resolveDaggerDependency() {
    (requireActivity() as PhotosActivity).activityComponent
      .inject(this)
  }

  companion object {
    fun newInstance(): ReceivedPhotosFragment {
      val fragment = ReceivedPhotosFragment()
      val args = Bundle()

      fragment.arguments = args
      return fragment
    }
  }
}
