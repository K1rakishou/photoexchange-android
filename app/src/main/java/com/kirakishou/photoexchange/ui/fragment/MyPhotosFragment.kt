package com.kirakishou.photoexchange.ui.fragment

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.Toast
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.PhotoUploadEvent
import com.kirakishou.photoexchange.mvp.viewmodel.AllPhotosActivityViewModel
import com.kirakishou.photoexchange.ui.activity.AllPhotosActivity
import com.kirakishou.photoexchange.ui.adapter.MyPhotosAdapter
import com.kirakishou.photoexchange.ui.adapter.MyPhotosAdapterSpanSizeLookup
import com.kirakishou.photoexchange.ui.viewstate.MyPhotosFragmentViewState
import com.kirakishou.photoexchange.ui.viewstate.MyPhotosFragmentViewStateEvent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject


class MyPhotosFragment : BaseFragment() {

    @BindView(R.id.my_photos_list)
    lateinit var myPhotosList: RecyclerView

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var viewModel: AllPhotosActivityViewModel

    lateinit var adapter: MyPhotosAdapter

    private val TAG = "MyPhotosFragment"
    private val PHOTO_ADAPTER_VIEW_WIDTH = 288
    private val adapterButtonsClickSubject = PublishSubject.create<MyPhotosAdapter.MyPhotosAdapterButtonClickEvent>().toSerialized()
    private var viewState = MyPhotosFragmentViewState()

    override fun getContentView(): Int = R.layout.fragment_my_photos

    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
        initRx()
        initRecyclerView()
        loadPhotos()

        restoreMyPhotosFragmentFromViewState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewState.saveToBundle(outState)
    }

    private fun restoreMyPhotosFragmentFromViewState(savedInstanceState: Bundle?) {
        viewState = MyPhotosFragmentViewState()
            .also { it.loadFromBundle(savedInstanceState) }
    }

    override fun onFragmentViewDestroy() {
    }

    private fun initRecyclerView() {
        val columnsCount = AndroidUtils.calculateNoOfColumns(requireContext(), PHOTO_ADAPTER_VIEW_WIDTH)

        adapter = MyPhotosAdapter(requireContext(), imageLoader, adapterButtonsClickSubject)
        adapter.init()

        val layoutManager = GridLayoutManager(requireContext(), columnsCount)
        layoutManager.spanSizeLookup = MyPhotosAdapterSpanSizeLookup(adapter, columnsCount)

        myPhotosList.layoutManager = layoutManager
        myPhotosList.adapter = adapter
    }

    private fun initRx() {
        compositeDisposable += viewModel.onPhotoUploadEventSubject
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { event -> onUploadingEvent(event) }
            .doOnError { Timber.tag(TAG).e(it) }
            .subscribe()

        compositeDisposable += viewModel.myPhotosFragmentViewStateSubject
            .observeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { viewState -> onViewStateChanged(viewState) }
            .doOnError { Timber.tag(TAG).e(it) }
            .subscribe()

        compositeDisposable += adapterButtonsClickSubject
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { Timber.tag(TAG).e(it) }
            .subscribe(viewModel.myPhotosAdapterButtonClickSubject::onNext)
    }

    private fun loadPhotos() {
        compositeDisposable += viewModel.loadMyPhotos()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { photos -> addPhotosToAdapter(photos) }
            .doOnError { Timber.tag(TAG).e(it) }
            .subscribe()
    }

    private fun onViewStateChanged(viewStateEvent: MyPhotosFragmentViewStateEvent) {
        if (!isAdded) {
            return
        }

        requireActivity().runOnUiThread {
            when (viewStateEvent) {
                is MyPhotosFragmentViewStateEvent.ScrollToTop -> {
                    myPhotosList.scrollToPosition(0)
                }
                is MyPhotosFragmentViewStateEvent.Default -> {

                }
                is MyPhotosFragmentViewStateEvent.ShowObtainCurrentLocationNotification -> {
                    adapter.showObtainCurrentLocationNotification()
                }
                is MyPhotosFragmentViewStateEvent.HideObtainCurrentLocationNotification -> {
                    adapter.hideObtainCurrentLocationNotification()
                }
                is MyPhotosFragmentViewStateEvent.RemovePhoto -> {
                    adapter.removePhotoById(viewStateEvent.photo.id)
                }
                is MyPhotosFragmentViewStateEvent.AddPhoto -> {
                    adapter.addMyPhoto(viewStateEvent.photo)
                }
                else -> throw IllegalArgumentException("Unknown MyPhotosFragmentViewStateEvent $viewStateEvent")
            }
        }
    }

    private fun onUploadingEvent(event: PhotoUploadEvent) {
        if (!isAdded) {
            return
        }

        requireActivity().runOnUiThread {
            when (event) {
                is PhotoUploadEvent.OnLocationUpdateStart -> {
                    Timber.tag(TAG).d("OnLocationUpdateStart")
                    adapter.showObtainCurrentLocationNotification()
                }
                is PhotoUploadEvent.OnLocationUpdateEnd -> {
                    Timber.tag(TAG).d("OnLocationUpdateEnd")
                    adapter.hideObtainCurrentLocationNotification()
                }
                is PhotoUploadEvent.OnCouldNotGetUserIdFromUserver -> {
                    Timber.tag(TAG).e("Could not get user id from the server")
                    showToast("Could not get user id from the server")
                }
                is PhotoUploadEvent.OnPrepare -> {

                }
                is PhotoUploadEvent.OnPhotoUploadStart -> {
                    Timber.tag(TAG).d("OnPhotoUploadStart, photoId = ${event.photo.id}")
                    adapter.addMyPhoto(event.photo.also { it.photoState = PhotoState.PHOTO_UPLOADING })
                }
                is PhotoUploadEvent.OnProgress -> {
                    adapter.addMyPhoto(event.photo)
                    adapter.updatePhotoProgress(event.photo.id, event.progress)
                }
                is PhotoUploadEvent.OnUploaded -> {
                    Timber.tag(TAG).d("OnUploaded, photoId = ${event.photo.id}")
                    adapter.removePhotoById(event.photo.id)
                    adapter.addMyPhoto(event.photo.also { it.photoState = PhotoState.PHOTO_UPLOADED })
                }
                is PhotoUploadEvent.OnFailedToUpload -> {
                    Timber.tag(TAG).d("OnFailedToUpload, photoId = ${event.photo.id}")
                    adapter.removePhotoById(event.photo.id)
                    adapter.addMyPhoto(event.photo.also { it.photoState = PhotoState.FAILED_TO_UPLOAD })
                }
                is PhotoUploadEvent.OnFoundPhotoAnswer -> {
                    adapter.updatePhotoState(event.photoId, PhotoState.PHOTO_UPLOADED_ANSWER_RECEIVED)
                }
                is PhotoUploadEvent.OnEnd -> {
                }
                is PhotoUploadEvent.OnUnknownError -> {
                    adapter.updateAllPhotosState(PhotoState.FAILED_TO_UPLOAD)
                }
                else -> throw IllegalArgumentException("Unknown PhotoUploadEvent $event")
            }
        }
    }

    private fun addPhotosToAdapter(uploadedPhotos: List<MyPhoto>) {
        if (!isAdded) {
            return
        }

        requireActivity().runOnUiThread {
            if (uploadedPhotos.isNotEmpty()) {
                adapter.clear()
                adapter.addMyPhotos(uploadedPhotos)
            } else {
                //TODO: show notification that no photos has been uploaded yet
            }
        }
    }

    private fun showToast(message: String, duration: Int = Toast.LENGTH_LONG) {
        (requireActivity() as AllPhotosActivity).showToast(message, duration)
    }

    override fun resolveDaggerDependency() {
        (requireActivity() as AllPhotosActivity).activityComponent
            .inject(this)
    }

    companion object {
        fun newInstance(): MyPhotosFragment {
            val fragment = MyPhotosFragment()
            val args = Bundle()

            fragment.arguments = args
            return fragment
        }
    }
}
