package com.kirakishou.photoexchange.ui.fragment


import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseFragment
import com.kirakishou.photoexchange.di.component.DaggerAllPhotoViewActivityComponent
import com.kirakishou.photoexchange.di.module.AllPhotoViewActivityModule
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mwvm.model.other.AdapterItem
import com.kirakishou.photoexchange.mwvm.model.other.AdapterItemType
import com.kirakishou.photoexchange.mwvm.model.other.Constants.PHOTO_ADAPTER_VIEW_WIDTH
import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto
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

    val cancelButtonSubject = PublishSubject.create<TakenPhoto>()
    val retryButtonSubject = PublishSubject.create<TakenPhoto>()

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
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onCancelButtonClick, this::onUnknownError)

        compositeDisposable += getViewModel().outputs.onQueuedUpPhotosLoadedObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onQueuedUpPhotosLoaded, this::onUnknownError)

        compositeDisposable += getViewModel().outputs.onShowPhotoUploadedOutputObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onPhotoUploaded, this::onUnknownError)

        compositeDisposable += getViewModel().outputs.onShowFailedToUploadPhotoObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onFailedToUploadPhoto, this::onUnknownError)

        compositeDisposable += getViewModel().outputs.onStartUploadingPhotosObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onStartUploadingPhotos() }, this::onUnknownError)

        compositeDisposable += getViewModel().outputs.onTakenPhotoUploadingCanceledObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onTakenPhotoUploadingCanceled, this::onUnknownError)

        compositeDisposable += getViewModel().outputs.onAllPhotosUploadedObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onAllPhotosUploaded() }, this::onUnknownError)
    }

    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
        initRecyclerView()
        showQueuedUpPhotos()
    }

    override fun onFragmentViewDestroy() {
    }

    private fun initRecyclerView() {
        val columnsCount = AndroidUtils.calculateNoOfColumns(activity!!, PHOTO_ADAPTER_VIEW_WIDTH)

        adapter = QueuedUpPhotosAdapter(activity!!, cancelButtonSubject, retryButtonSubject)
        adapter.init()

        val layoutManager = GridLayoutManager(activity, columnsCount)
        layoutManager.spanSizeLookup = QueuedUpPhotosAdapterSpanSizeLookup(adapter, columnsCount)

        queuedUpPhotosRv.layoutManager = layoutManager
        queuedUpPhotosRv.clearOnScrollListeners()
        queuedUpPhotosRv.adapter = adapter
        queuedUpPhotosRv.setHasFixedSize(true)
    }

    private fun showQueuedUpPhotos() {
        getViewModel().inputs.getQueuedUpPhotos()
    }

    private fun onCancelButtonClick(takenPhoto: TakenPhoto) {
        getViewModel().inputs.cancelTakenPhotoUploading(takenPhoto.id)
    }

    private fun onRetryButtonClicked(takenPhoto: TakenPhoto) {
        Timber.d("QueuedUpPhotosListFragment: photoName: ${takenPhoto.photoName}")
    }

    private fun onTakenPhotoUploadingCanceled(id: Long) {
        Timber.d("QueuedUpPhotosListFragment: onTakenPhotoUploadingCanceled()")

        adapter.runOnAdapterHandler {
            adapter.removeQueuedUpPhoto(id)

            if (adapter.itemCount == 0) {
                adapter.addMessage(QueuedUpPhotosAdapter.MESSAGE_TYPE_NO_PHOTOS_TO_UPLOAD)
            }
        }
    }

    private fun onQueuedUpPhotosLoaded(queuedUpPhotosList: List<TakenPhoto>) {
        Timber.d("QueuedUpPhotosListFragment: onQueuedUpPhotosLoaded()")

        adapter.runOnAdapterHandler {
            adapter.clear()

            if (queuedUpPhotosList.isNotEmpty()) {
                adapter.addQueuedUpPhotos(queuedUpPhotosList)
            } else {
                adapter.addMessage(QueuedUpPhotosAdapter.MESSAGE_TYPE_NO_PHOTOS_TO_UPLOAD)
            }
        }
    }

    private fun onStartUploadingPhotos() {
        Timber.d("QueuedUpPhotosListFragment: onStartUploadingPhotos()")

        adapter.runOnAdapterHandler {
            adapter.removeMessage()
            adapter.setButtonsEnabled(false)
        }
    }

    private fun onPhotoUploaded(photo: TakenPhoto) {
        Timber.d("QueuedUpPhotosListFragment: onPhotoUploaded()")

        adapter.runOnAdapterHandler {
            adapter.removeQueuedUpPhoto(photo.id)
        }
    }

    private fun onAllPhotosUploaded() {
        Timber.d("QueuedUpPhotosListFragment: onAllPhotosUploaded()")

        adapter.runOnAdapterHandler {
            adapter.addMessage(QueuedUpPhotosAdapter.MESSAGE_TYPE_ALL_PHOTOS_UPLOADED)
            adapter.setButtonsEnabled(true)
        }
    }

    private fun onFailedToUploadPhoto(photo: TakenPhoto) {
        Timber.d("QueuedUpPhotosListFragment: onFailedToUploadPhoto()")

        adapter.runOnAdapterHandler {
            adapter.removeQueuedUpPhoto(photo.id)
            adapter.add(AdapterItem(photo, AdapterItemType.VIEW_FAILED_TO_UPLOAD))
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
