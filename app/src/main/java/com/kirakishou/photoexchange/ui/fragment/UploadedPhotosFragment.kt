package com.kirakishou.photoexchange.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.withState
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.IntercomListener
import com.kirakishou.photoexchange.helper.intercom.StateEventListener
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.state.UploadedPhotosFragmentState
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.adapter.epoxy.failedToUploadPhotoRow
import com.kirakishou.photoexchange.ui.adapter.epoxy.queuedUpPhotoRow
import com.kirakishou.photoexchange.ui.adapter.epoxy.uploadingPhotoRow
import timber.log.Timber
import javax.inject.Inject


class UploadedPhotosFragment : BaseMvRxFragment(), StateEventListener<UploadedPhotosFragmentEvent>, IntercomListener {
  @BindView(R.id.my_photos_list)
  lateinit var uploadedPhotosList: RecyclerView

  @Inject
  lateinit var imageLoader: ImageLoader

  @Inject
  lateinit var viewModel: PhotosActivityViewModel

//  lateinit var adapter: UploadedPhotosAdapter
//  lateinit var endlessScrollListener: EndlessRecyclerOnScrollListener

  private val TAG = "UploadedPhotosFragment"
//  private val PHOTO_ADAPTER_VIEW_WIDTH = DEFAULT_ADAPTER_ITEM_WIDTH
//  private val failedToUploadPhotoButtonClicksSubject = PublishSubject.create<UploadedPhotosAdapter.UploadedPhotosAdapterButtonClick>().toSerialized()
//
//  private val loadMoreSubject = PublishSubject.create<Unit>()
//  private val scrollSubject = PublishSubject.create<Boolean>()
//
//  private var photosPerPage = 0

  override fun getFragmentLayoutId(): Int = R.layout.fragment_mvrx

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    viewModel.uploadedPhotosFragmentViewModel.selectSubscribe(this, UploadedPhotosFragmentState::takenPhotos) { takenPhotos ->
      viewModel.intercom.tell<PhotosActivity>()
        .to(PhotosActivityEvent.StartUploadingService(PhotosActivityViewModel::class.java,
          "There are queued up photos in the database"))
    }

    viewModel.uploadedPhotosFragmentViewModel.selectSubscribe(this, UploadedPhotosFragmentState::uploadedPhotos) {
      viewModel.intercom.tell<PhotosActivity>()
        .to(PhotosActivityEvent.StartReceivingService(PhotosActivityViewModel::class.java,
          "Starting the service after a page of uploaded photos was loaded"))
    }
  }

  override fun buildEpoxyController(): AsyncEpoxyController = simpleController {
    return@simpleController withState(viewModel.uploadedPhotosFragmentViewModel) { state ->
      when (state.takenPhotosRequest) {
        is Success -> {
          Timber.tag(TAG).d("Success")

          if (state.takenPhotos.isNotEmpty()) {
            state.takenPhotos
              .forEach { photo ->
                when (photo.photoState) {
                  PhotoState.PHOTO_TAKEN -> {
                  }
                  PhotoState.PHOTO_QUEUED_UP -> {
                    queuedUpPhotoRow {
                      id(photo.id)
                      photo(photo)
                    }
                  }
                  PhotoState.PHOTO_UPLOADING -> {
                    uploadingPhotoRow {
                      id(photo.id)
                      photo(photo)
                      progress(50)
                    }
                  }
                  PhotoState.FAILED_TO_UPLOAD -> {
                    failedToUploadPhotoRow {
                      id(photo.id)
                      photo(photo)
                      deleteButtonCallback { _ ->
                        println("delete clicked")
                      }
                      retryButtonCallback { _ ->
                        println("retry clicked")
                      }
                    }
                  }
                }
              }
          }
        }
        is Fail -> {
          Timber.tag(TAG).d("Fail")
        }
        is Loading -> {
          Timber.tag(TAG).d("Loading")
        }
        else -> {
          //do nothing when Uninitialized
        }
      }
    }
  }

//  override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
//    viewModel.uploadedPhotosFragmentViewModel.viewState.reset()
//
//    launch { initRx() }
//    initRecyclerView()
//  }
//
//  override fun onFragmentViewDestroy() {
//  }

//  private suspend fun initRx() {
//    compositeDisposable += viewModel.uploadedPhotosFragmentViewModel.knownErrors
//      .subscribe({ errorCode -> handleKnownErrors(errorCode) })
//
//    compositeDisposable += viewModel.uploadedPhotosFragmentViewModel.unknownErrors
//      .subscribe({ error -> handleUnknownErrors(error) })
//
//    compositeDisposable += failedToUploadPhotoButtonClicksSubject
//      .observeOn(AndroidSchedulers.mainThread())
//      .subscribe({
//        viewModel.intercom.tell<PhotosActivity>()
//          .that(PhotosActivityEvent.FailedToUploadPhotoButtonClicked(it))
//      }, { Timber.tag(TAG).e(it) })
//
//    compositeDisposable += loadMoreSubject
//      .subscribe({ viewModel.uploadedPhotosFragmentViewModel.loadMorePhotos() })
//
//    compositeDisposable += scrollSubject
//      .subscribeOn(Schedulers.io())
//      .distinctUntilChanged()
//      .throttleFirst(200, TimeUnit.MILLISECONDS)
//      .subscribe({ isScrollingDown ->
//        viewModel.intercom.tell<PhotosActivity>()
//          .that(PhotosActivityEvent.ScrollEvent(isScrollingDown))
//      })
//
//    compositeChannel += viewModel.intercom.uploadedPhotosFragmentEvents.listen().openSubscription().apply {
//      consumeEach { event -> onStateEvent(event) }
//    }
//  }

  private fun initRecyclerView() {
//    val columnsCount = AndroidUtils.calculateNoOfColumns(requireContext(), PHOTO_ADAPTER_VIEW_WIDTH)
//
//    adapter = UploadedPhotosAdapter(requireContext(), imageLoader, failedToUploadPhotoButtonClicksSubject)
//
//    val layoutManager = GridLayoutManager(requireContext(), columnsCount)
//    layoutManager.spanSizeLookup = UploadedPhotosAdapterSpanSizeLookup(adapter, columnsCount)
//    photosPerPage = Constants.UPLOADED_PHOTOS_PER_ROW * layoutManager.spanCount
//
//    //TODO: visible threshold should be less than photosPerPage count
//    endlessScrollListener = EndlessRecyclerOnScrollListener(TAG, layoutManager, 2, loadMoreSubject, scrollSubject)
//
//    uploadedPhotosList.layoutManager = layoutManager
//    uploadedPhotosList.adapter = adapter
//    uploadedPhotosList.clearOnScrollListeners()
//    uploadedPhotosList.addOnScrollListener(endlessScrollListener)
  }

//  private fun triggerPhotosLoading() {
//    loadMoreSubject.onNext(Unit)
//  }

  override suspend fun onStateEvent(event: UploadedPhotosFragmentEvent) {
    if (!isAdded) {
      return
    }

    when (event) {
      is UploadedPhotosFragmentEvent.GeneralEvents -> {
        onUiEvent(event)
      }

      is UploadedPhotosFragmentEvent.PhotoUploadEvent -> {
        viewModel.uploadedPhotosFragmentViewModel.onUploadingEvent(event)
      }
    }.safe
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
        is UploadedPhotosFragmentEvent.GeneralEvents.RemovePhoto -> {
//          adapter.removePhotoById(event.photo.id)
        }
        is UploadedPhotosFragmentEvent.GeneralEvents.AddPhoto -> {
//          adapter.addTakenPhoto(event.photo)
        }
        is UploadedPhotosFragmentEvent.GeneralEvents.PhotoRemoved -> {
//          if (adapter.getQueuedUpAndFailedPhotosCount() == 0) {
//            triggerPhotosLoading()
//          } else {
//            //do nothing
//          }
        }
        is UploadedPhotosFragmentEvent.GeneralEvents.AfterPermissionRequest -> {
//          triggerPhotosLoading()
        }
        is UploadedPhotosFragmentEvent.GeneralEvents.UpdateReceiverInfo -> {
//          event.receivedPhotos.forEach {
//            adapter.updateUploadedPhotoSetReceiverInfo(it.uploadedPhotoName)
//          }
        }
        is UploadedPhotosFragmentEvent.GeneralEvents.OnPageSelected -> {
//          viewModel.uploadedPhotosFragmentViewModel.viewState.reset()
        }
        is UploadedPhotosFragmentEvent.GeneralEvents.ShowTakenPhotos -> {
//          addTakenPhotosToAdapter(event.takenPhotos)
        }
        is UploadedPhotosFragmentEvent.GeneralEvents.ShowUploadedPhotos -> {
//          addUploadedPhotosToAdapter(event.uploadedPhotos)
        }
        is UploadedPhotosFragmentEvent.GeneralEvents.PhotoReceived -> {
          //TODO
//          adapter.updateUploadedPhotoSetReceiverInfo(event.takenPhotoName)
        }
      }.safe
    }
  }

//  private fun addUploadedPhotosToAdapter(uploadedPhotos: List<UploadedPhoto>) {
//    if (!isAdded) {
//      return
//    }
//
//    uploadedPhotosList.post {
//      if (uploadedPhotos.isNotEmpty()) {
//        adapter.addUploadedPhotos(uploadedPhotos)
//      }
//
//      endlessScrollListener.pageLoaded()
//
//      if (adapter.getUploadedPhotosCount() == 0) {
//        adapter.showMessageFooter("You have no uploaded photos")
//        return@post
//      }
//
//      if (uploadedPhotos.size < photosPerPage) {
//        adapter.showMessageFooter("End of the list reached")
//        endlessScrollListener.reachedEnd()
//      }
//    }
//  }

//  private fun addTakenPhotosToAdapter(takenPhotos: List<TakenPhoto>) {
//    if (!isAdded) {
//      return
//    }
//
//    uploadedPhotosList.post {
//      if (takenPhotos.isNotEmpty()) {
//        adapter.clear()
//        adapter.addTakenPhotos(takenPhotos)
//      } else {
//        adapter.showMessageFooter("You have no taken photos")
//      }
//    }
//  }
//
//  private fun showProgressFooter() {
//    if (!isAdded) {
//      return
//    }
//
//    uploadedPhotosList.post {
//      adapter.showProgressFooter()
//    }
//  }
//
//  private fun hideProgressFooter() {
//    if (!isAdded) {
//      return
//    }
//
//    uploadedPhotosList.post {
//      adapter.clearFooter()
//    }
//  }

//  private fun handleKnownErrors(errorCode: ErrorCode) {
//    //TODO: do we even need this method?
//    hideProgressFooter()
//    adapter.updateAllPhotosState(PhotoState.FAILED_TO_UPLOAD)
//    (activity as? PhotosActivity)?.showKnownErrorMessage(errorCode)
//  }
//
//  private fun handleUnknownErrors(error: Throwable) {
//    adapter.updateAllPhotosState(PhotoState.FAILED_TO_UPLOAD)
//    (activity as? PhotosActivity)?.showUnknownErrorMessage(error)
//
//    Timber.tag(TAG).e(error)
//  }

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
