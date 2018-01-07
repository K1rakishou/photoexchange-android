package com.kirakishou.photoexchange.ui.fragment


import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.Toast
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseFragment
import com.kirakishou.photoexchange.di.component.DaggerAllPhotoViewActivityComponent
import com.kirakishou.photoexchange.di.module.AllPhotoViewActivityModule
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.extension.filterMulticastEvent
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mwvm.model.other.AdapterItem
import com.kirakishou.photoexchange.mwvm.model.other.AdapterItemType
import com.kirakishou.photoexchange.mwvm.model.other.Constants.PHOTO_ADAPTER_VIEW_WIDTH
import com.kirakishou.photoexchange.mwvm.model.state.PhotoState
import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto
import com.kirakishou.photoexchange.mwvm.model.state.PhotoUploadingState
import com.kirakishou.photoexchange.mwvm.viewmodel.AllPhotosViewActivityViewModel
import com.kirakishou.photoexchange.mwvm.viewmodel.factory.AllPhotosViewActivityViewModelFactory
import com.kirakishou.photoexchange.ui.activity.AllPhotosViewActivity
import com.kirakishou.photoexchange.ui.adapter.QueuedUpPhotosAdapter
import com.kirakishou.photoexchange.ui.widget.QueuedUpPhotosAdapterSpanSizeLookup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject


class QueuedUpPhotosListFragment : BaseFragment<AllPhotosViewActivityViewModel>() {

    @BindView(R.id.queued_up_photos_list)
    lateinit var queuedUpPhotosRv: RecyclerView

    @Inject
    lateinit var viewModelFactory: AllPhotosViewActivityViewModelFactory

    @Inject
    lateinit var imageLoader: ImageLoader

    private val ttag = "[${this::class.java.simpleName}]: "
    private val cancelButtonSubject = PublishSubject.create<TakenPhoto>()
    private val retryButtonSubject = PublishSubject.create<TakenPhoto>()

    lateinit var adapter: QueuedUpPhotosAdapter

    override fun initViewModel(): AllPhotosViewActivityViewModel {
        return ViewModelProviders.of(activity!!, viewModelFactory).get(AllPhotosViewActivityViewModel::class.java)
    }

    override fun getContentView(): Int = R.layout.fragment_queued_up_photos_list

    override fun initRx() {
        compositeDisposable += retryButtonSubject
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onRetryButtonClicked, this::onUnknownError)

        compositeDisposable += cancelButtonSubject
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onCancelButtonClick, this::onUnknownError)

        compositeDisposable += getViewModel().outputs.onQueuedUpAndFailedToUploadLoadedObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onQueuedUpAndFailedToUploadLoaded, this::onUnknownError)

        compositeDisposable += getViewModel().outputs.onPhotoUploadingStateObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .filterMulticastEvent(QueuedUpPhotosListFragment::class.java)
                .subscribe(this::onPhotoUploadingState, this::onUnknownError)

        compositeDisposable += getViewModel().outputs.onTakenPhotoUploadingCanceledObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onTakenPhotoUploadingCanceled, this::onUnknownError)

        compositeDisposable += getViewModel().errors.onUnknownErrorObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onUnknownError)
    }

    private fun onPhotoUploadingState(state: PhotoUploadingState) {
        when (state) {
            is PhotoUploadingState.StartPhotoUploading -> {
                Timber.tag(ttag).d("onPhotoUploadingState() PhotoUploadingState.StartPhotoUploading")

                adapter.runOnAdapterHandler {
                    adapter.removeMessage()
                    adapter.setButtonsEnabled(false)
                }
            }

            is PhotoUploadingState.PhotoUploaded -> {
                Timber.tag(ttag).d("onPhotoUploadingState() PhotoUploadingState.PhotoUploaded")

                adapter.runOnAdapterHandler {
                    adapter.removeQueuedUpPhoto(state.photo!!.id)
                }
            }

            is PhotoUploadingState.AllPhotosUploaded -> {
                Timber.tag(ttag).d("onPhotoUploadingState() PhotoUploadingState.AllPhotosUploaded")

                adapter.runOnAdapterHandler {
                    if (!adapter.containsFailedToUploadPhotos()) {
                        adapter.addMessage(QueuedUpPhotosAdapter.MESSAGE_TYPE_ALL_PHOTOS_UPLOADED)
                    } else {
                        Toast.makeText(activity, "Could not upload one or more photos", Toast.LENGTH_LONG).show()
                    }

                    adapter.setButtonsEnabled(true)
                }
            }

            is PhotoUploadingState.FailedToUploadPhoto -> {
                Timber.tag(ttag).d("onPhotoUploadingState() PhotoUploadingState.FailedToUploadPhoto")

                adapter.runOnAdapterHandler {
                    adapter.removeQueuedUpPhoto(state.photo.id)
                    adapter.add(AdapterItem(state.photo, AdapterItemType.VIEW_FAILED_TO_UPLOAD))
                }
            }

            else -> IllegalStateException("Bad value")
        }
    }

    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
        initRecyclerView()
        showQueuedUpAndFailedToUploadPhotos()
    }

    override fun onFragmentViewDestroy() {
    }

    override fun onResume() {
        super.onResume()

        getViewModel().inputs.beginReceivingEvents(this::class.java)
        getViewModel().inputs.startPhotosUploading()
    }

    override fun onPause() {
        super.onPause()

        getViewModel().inputs.stopReceivingEvents(this::class.java)
    }

    private fun initRecyclerView() {
        val columnsCount = AndroidUtils.calculateNoOfColumns(activity!!, PHOTO_ADAPTER_VIEW_WIDTH)

        adapter = QueuedUpPhotosAdapter(activity!!, imageLoader, cancelButtonSubject, retryButtonSubject)
        adapter.init()

        val layoutManager = GridLayoutManager(activity, columnsCount)
        layoutManager.spanSizeLookup = QueuedUpPhotosAdapterSpanSizeLookup(adapter, columnsCount)

        queuedUpPhotosRv.layoutManager = layoutManager
        queuedUpPhotosRv.clearOnScrollListeners()
        queuedUpPhotosRv.adapter = adapter
        queuedUpPhotosRv.setHasFixedSize(true)
    }

    private fun showQueuedUpAndFailedToUploadPhotos() {
        getViewModel().inputs.getQueuedUpAndFailedToUploadPhotos()
    }

    private fun onCancelButtonClick(takenPhoto: TakenPhoto) {
        getViewModel().inputs.cancelTakenPhotoUploading(takenPhoto.id)
    }

    private fun onRetryButtonClicked(takenPhoto: TakenPhoto) {
        Timber.tag(ttag).d("onRetryButtonClicked() photoName: ${takenPhoto.photoName}")

        adapter.runOnAdapterHandler {
            adapter.removeFailedToUploadPhoto(takenPhoto.id)
            adapter.add(AdapterItem(takenPhoto, AdapterItemType.VIEW_QUEUED_UP_PHOTO))
        }

        getViewModel().markPhotoToBeUploadedAgain(takenPhoto.id)
    }

    private fun onTakenPhotoUploadingCanceled(id: Long) {
        Timber.tag(ttag).d("onTakenPhotoUploadingCanceled()")

        adapter.runOnAdapterHandler {
            adapter.removeQueuedUpPhoto(id)

            if (adapter.itemCount == 0) {
                adapter.addMessage(QueuedUpPhotosAdapter.MESSAGE_TYPE_NO_PHOTOS_TO_UPLOAD)
            }
        }
    }

    private fun onQueuedUpAndFailedToUploadLoaded(photosList: List<TakenPhoto>) {
        Timber.tag(ttag).d("onQueuedUpAndFailedToUploadLoaded()")

        val failedToUploadPhotos = photosList.filter { it.photoState == PhotoState.FAILED_TO_UPLOAD }
        val queuedUpPhotos = photosList.filter { it.photoState == PhotoState.QUEUED_UP }

        adapter.runOnAdapterHandler {
            adapter.clear()

            if (failedToUploadPhotos.isEmpty() && queuedUpPhotos.isEmpty()) {
                adapter.addMessage(QueuedUpPhotosAdapter.MESSAGE_TYPE_NO_PHOTOS_TO_UPLOAD)
            } else {
                failedToUploadPhotos.forEach { photo ->
                    adapter.add(AdapterItem(photo, AdapterItemType.VIEW_FAILED_TO_UPLOAD))
                }

                if (queuedUpPhotos.isNotEmpty()) {
                    adapter.addQueuedUpPhotos(queuedUpPhotos)
                }
            }
        }
    }

    private fun onUnknownError(error: Throwable) {
        (activity as AllPhotosViewActivity).onUnknownError(error)
    }

    override fun resolveDaggerDependency() {
        DaggerAllPhotoViewActivityComponent.builder()
                .applicationComponent(PhotoExchangeApplication.applicationComponent)
                .allPhotoViewActivityModule(AllPhotoViewActivityModule(activity as AllPhotosViewActivity))
                .build()
                .inject(this)
    }

    companion object {
        fun newInstance(): QueuedUpPhotosListFragment {
            val fragment = QueuedUpPhotosListFragment()
            val args = Bundle()

            fragment.arguments = args
            return fragment
        }
    }
}
