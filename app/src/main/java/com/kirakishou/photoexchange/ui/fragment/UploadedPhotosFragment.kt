package com.kirakishou.photoexchange.ui.fragment

import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
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
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.adapter.UploadedPhotosAdapter
import com.kirakishou.photoexchange.ui.adapter.UploadedPhotosAdapterSpanSizeLookup
import com.kirakishou.photoexchange.ui.widget.EndlessRecyclerOnScrollListener
import core.ErrorCode
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class UploadedPhotosFragment : BaseFragment(), StateEventListener<UploadedPhotosFragmentEvent>, IntercomListener {

  @BindView(R.id.my_photos_list)
  lateinit var uploadedPhotosList: RecyclerView

  @Inject
  lateinit var imageLoader: ImageLoader

  @Inject
  lateinit var viewModel: PhotosActivityViewModel

  lateinit var adapter: UploadedPhotosAdapter
  lateinit var endlessScrollListener: EndlessRecyclerOnScrollListener

  private val TAG = "UploadedPhotosFragment"
  private val PHOTO_ADAPTER_VIEW_WIDTH = DEFAULT_ADAPTER_ITEM_WIDTH
  private val failedToUploadPhotoButtonClicksSubject = PublishSubject.create<UploadedPhotosAdapter.UploadedPhotosAdapterButtonClick>().toSerialized()

  private val loadMoreSubject = PublishSubject.create<Unit>()
  private val scrollSubject = PublishSubject.create<Boolean>()

  private var photosPerPage = 0

  override fun getContentView(): Int = R.layout.fragment_uploaded_photos

  override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
    viewModel.uploadedPhotosFragmentViewModel.viewState.reset()

    initRx()
    initRecyclerView()
  }

  override fun onFragmentViewDestroy() {
  }

  private fun initRx() {
    compositeDisposable += viewModel.intercom.uploadedPhotosFragmentEvents.listen()
      .subscribe({ viewState -> onStateEvent(viewState) }, { Timber.tag(TAG).e(it) })

    compositeDisposable += viewModel.uploadedPhotosFragmentViewModel.knownErrors
      .subscribe({ errorCode -> handleKnownErrors(errorCode) })

    compositeDisposable += viewModel.uploadedPhotosFragmentViewModel.unknownErrors
      .subscribe({ error -> handleUnknownErrors(error) })

    compositeDisposable += failedToUploadPhotoButtonClicksSubject
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe({
        viewModel.intercom.tell<PhotosActivity>()
          .that(PhotosActivityEvent.FailedToUploadPhotoButtonClicked(it))
      }, { Timber.tag(TAG).e(it) })

    compositeDisposable += loadMoreSubject
      .subscribe({ viewModel.uploadedPhotosFragmentViewModel.loadMorePhotos() })

    compositeDisposable += scrollSubject
      .subscribeOn(Schedulers.io())
      .distinctUntilChanged()
      .throttleFirst(200, TimeUnit.MILLISECONDS)
      .subscribe({ isScrollingDown ->
        viewModel.intercom.tell<PhotosActivity>()
          .that(PhotosActivityEvent.ScrollEvent(isScrollingDown))
      })
  }

  private fun initRecyclerView() {
    val columnsCount = AndroidUtils.calculateNoOfColumns(requireContext(), PHOTO_ADAPTER_VIEW_WIDTH)

    adapter = UploadedPhotosAdapter(requireContext(), imageLoader, failedToUploadPhotoButtonClicksSubject)

    val layoutManager = GridLayoutManager(requireContext(), columnsCount)
    layoutManager.spanSizeLookup = UploadedPhotosAdapterSpanSizeLookup(adapter, columnsCount)
    photosPerPage = Constants.UPLOADED_PHOTOS_PER_ROW * layoutManager.spanCount

    //TODO: visible threshold should be less than photosPerPage count
    endlessScrollListener = EndlessRecyclerOnScrollListener(TAG, layoutManager, 2, loadMoreSubject, scrollSubject)

    uploadedPhotosList.layoutManager = layoutManager
    uploadedPhotosList.adapter = adapter
    uploadedPhotosList.clearOnScrollListeners()
    uploadedPhotosList.addOnScrollListener(endlessScrollListener)
  }

  private fun triggerPhotosLoading() {
    loadMoreSubject.onNext(Unit)
  }

  override fun onStateEvent(event: UploadedPhotosFragmentEvent) {
    if (!isAdded) {
      return
    }

    uploadedPhotosList.post {
      when (event) {
        is UploadedPhotosFragmentEvent.GeneralEvents -> {
          onUiEvent(event)
        }

        is UploadedPhotosFragmentEvent.PhotoUploadEvent -> {
          onUploadingEvent(event)
        }
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
        is UploadedPhotosFragmentEvent.GeneralEvents.PhotoRemoved -> {
          if (adapter.getQueuedUpAndFailedPhotosCount() == 0) {
            triggerPhotosLoading()
          } else {
            //do nothing
          }
        }
        is UploadedPhotosFragmentEvent.GeneralEvents.AfterPermissionRequest -> {
          triggerPhotosLoading()
        }
        is UploadedPhotosFragmentEvent.GeneralEvents.UpdateReceiverInfo -> {
          event.receivedPhotos.forEach {
            adapter.updateUploadedPhotoSetReceiverInfo(it.uploadedPhotoName)
          }
        }
        is UploadedPhotosFragmentEvent.GeneralEvents.OnPageSelected -> {
          viewModel.uploadedPhotosFragmentViewModel.viewState.reset()
        }
        is UploadedPhotosFragmentEvent.GeneralEvents.ShowTakenPhotos -> {
          addTakenPhotosToAdapter(event.takenPhotos)
        }
        is UploadedPhotosFragmentEvent.GeneralEvents.ShowUploadedPhotos -> {
          addUploadedPhotosToAdapter(event.uploadedPhotos)
        }
        is UploadedPhotosFragmentEvent.GeneralEvents.DisableEndlessScrolling -> {
          endlessScrollListener.reachedEnd()
        }
        is UploadedPhotosFragmentEvent.GeneralEvents.EnableEndlessScrolling -> {
          endlessScrollListener.reset()
        }
        is UploadedPhotosFragmentEvent.GeneralEvents.PageIsLoading -> {
//                    endlessScrollListener.pageLoading()
        }
      }.safe
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
        }
        is UploadedPhotosFragmentEvent.PhotoUploadEvent.PhotoReceived -> {
          adapter.updateUploadedPhotoSetReceiverInfo(event.takenPhotoName)
        }
        is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnEnd -> {
          loadMoreSubject.onNext(Unit)
        }
        is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnError -> {
          handleUnknownErrors(event.exception)
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
        viewModel.uploadedPhotosFragmentViewModel.viewState.updateLastId(uploadedPhotos.last().photoId)
        adapter.addUploadedPhotos(uploadedPhotos)
      }

      endlessScrollListener.pageLoaded()

      if (adapter.getUploadedPhotosCount() == 0) {
        adapter.showMessageFooter("You have no uploaded photos")
        return@post
      }

      if (uploadedPhotos.size < photosPerPage) {
        adapter.showMessageFooter("End of the list reached")
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
        adapter.showMessageFooter("You have no taken photos")
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
      adapter.clearFooter()
    }
  }

  private fun handleKnownErrors(errorCode: ErrorCode) {
    //TODO: do we even need this method?
    hideProgressFooter()
    adapter.updateAllPhotosState(PhotoState.FAILED_TO_UPLOAD)
    (activity as? PhotosActivity)?.showKnownErrorMessage(errorCode)
  }

  private fun handleUnknownErrors(error: Throwable) {
    adapter.updateAllPhotosState(PhotoState.FAILED_TO_UPLOAD)
    (activity as? PhotosActivity)?.showUnknownErrorMessage(error)

    Timber.tag(TAG).e(error)
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
