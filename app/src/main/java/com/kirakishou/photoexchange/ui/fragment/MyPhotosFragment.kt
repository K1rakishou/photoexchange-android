package com.kirakishou.photoexchange.ui.fragment

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.PhotoUploadingEvent
import com.kirakishou.photoexchange.mvp.viewmodel.AllPhotosActivityViewModel
import com.kirakishou.photoexchange.ui.activity.AllPhotosActivity
import com.kirakishou.photoexchange.ui.adapter.MyPhotosAdapter
import com.kirakishou.photoexchange.ui.adapter.MyPhotosAdapterSpanSizeLookup
import com.kirakishou.photoexchange.ui.viewstate.MyPhotosFragmentViewState
import com.kirakishou.photoexchange.ui.viewstate.MyPhotosFragmentViewStateEvent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
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

        if (viewState.showObtainCurrentLocationNotification) {
            adapter.showObtainCurrentLocationNotification()
        } else {
            adapter.hideObtainCurrentLocationNotification()
        }
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

    private fun loadPhotos() {
        compositeDisposable += viewModel.loadPhotos()
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { photos -> addPhotoToAdapter(photos) }
            .subscribe()
    }

    private fun initRx() {
        compositeDisposable += viewModel.onUploadingPhotoEventSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { event -> onUploadingEvent(event) }
            .subscribe()

        compositeDisposable += viewModel.myPhotosFragmentViewStateSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { viewState -> onViewStateChanged(viewState) }
            .subscribe()

        compositeDisposable += adapterButtonsClickSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap { adapterButtonsClickEvent ->
                val startUploadingService = when (adapterButtonsClickEvent) {
                    is MyPhotosAdapter.MyPhotosAdapterButtonClickEvent.DeleteButtonClick -> {
                        viewModel.deletePhotoById(adapterButtonsClickEvent.photo.id).blockingAwait()
                        adapter.removePhotoById(adapterButtonsClickEvent.photo.id)
                        false
                    }

                    is MyPhotosAdapter.MyPhotosAdapterButtonClickEvent.RetryButtonClick -> {
                        viewModel.changePhotoState(adapterButtonsClickEvent.photo.id, PhotoState.PHOTO_QUEUED_UP).blockingAwait()
                        adapter.removePhotoById(adapterButtonsClickEvent.photo.id)
                        adapter.addMyPhoto(adapterButtonsClickEvent.photo.also { it.photoState = PhotoState.PHOTO_QUEUED_UP })
                        true
                    }
                }

                return@flatMap Observable.just(startUploadingService)
            }
            .filter { startUploadingService -> startUploadingService }
            .map { Unit }
            .doOnNext { viewModel.checkShouldStartPhotoUploadingService(true) }
            .doOnError { Timber.e(it) }
            .subscribe()
    }

    private fun onViewStateChanged(viewStateEvent: MyPhotosFragmentViewStateEvent) {
        if (!isAdded) {
            return
        }

        requireActivity().runOnUiThread {
            viewState.updateFromViewStateEvent(viewStateEvent)

            when (viewStateEvent) {
                is MyPhotosFragmentViewStateEvent.Default -> {

                }
                is MyPhotosFragmentViewStateEvent.ShowObtainCurrentLocationNotification -> {
                    adapter.showObtainCurrentLocationNotification()
                }
                is MyPhotosFragmentViewStateEvent.HideObtainCurrentLocationNotification -> {
                    adapter.hideObtainCurrentLocationNotification()
                }
                is MyPhotosFragmentViewStateEvent.RemovePhotoById -> {
                    adapter.removePhotoById(viewStateEvent.photoId)
                }
                else -> throw IllegalArgumentException("Unknown MyPhotosFragmentViewStateEvent $viewStateEvent")
            }
        }
    }

    private fun scrollRecyclerViewToTop() {
        val layoutManager = (myPhotosList.layoutManager as GridLayoutManager)
        if (layoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
            myPhotosList.scrollToPosition(0)
        }
    }

    private fun onUploadingEvent(event: PhotoUploadingEvent) {
        if (!isAdded) {
            return
        }

        requireActivity().runOnUiThread {
            when (event) {
                is PhotoUploadingEvent.OnPrepare -> {
                    scrollRecyclerViewToTop()
                }
                is PhotoUploadingEvent.OnPhotoUploadingStart -> {
                    //TODO: make new adapter method "update photo"
                    adapter.removePhotoById(event.myPhoto.id)
                    adapter.addMyPhoto(event.myPhoto.also { it.photoState = PhotoState.PHOTO_UPLOADING })
                }
                is PhotoUploadingEvent.OnProgress -> {
                    adapter.updatePhotoProgress(event.photoId, event.progress)
                }
                is PhotoUploadingEvent.OnUploaded -> {
                    adapter.removePhotoById(event.myPhoto.id)
                    adapter.addMyPhoto(event.myPhoto.also { it.photoState = PhotoState.PHOTO_UPLOADED })
                }
                is PhotoUploadingEvent.OnEnd -> {
                }
                is PhotoUploadingEvent.OnFailedToUpload -> {
                    adapter.removePhotoById(event.myPhoto.id)
                    adapter.addMyPhoto(event.myPhoto)
                }
                is PhotoUploadingEvent.OnUnknownError -> {
                    adapter.clear()
                }
                else -> throw IllegalArgumentException("Unknown PhotoUploadingEvent $event")
            }
        }
    }

    private fun addPhotoToAdapter(uploadedPhotos: List<MyPhoto>) {
        if (!isAdded) {
            return
        }

        requireActivity().runOnUiThread {
            if (uploadedPhotos.isNotEmpty()) {
                adapter.addMyPhotos(uploadedPhotos)
            } else {
                //TODO: show notification that no photos has been uploaded yet
            }
        }
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
