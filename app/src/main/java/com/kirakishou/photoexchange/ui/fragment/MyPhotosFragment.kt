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
import com.kirakishou.photoexchange.ui.adapter.MyPhotosAdapterItem
import com.kirakishou.photoexchange.ui.viewstate.MyPhotosFragmentViewStateEvent
import com.kirakishou.photoexchange.ui.adapter.MyPhotosAdapterSpanSizeLookup
import com.kirakishou.photoexchange.ui.viewstate.MyPhotosFragmentViewState
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
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
    private var viewState = MyPhotosFragmentViewState()

    override fun getContentView(): Int = R.layout.fragment_my_photos

    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
        initRx()
        initRecyclerView()

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
        viewModel.resumeUploadingProcess()
    }

    private fun initRecyclerView() {
        val columnsCount = AndroidUtils.calculateNoOfColumns(requireContext(), PHOTO_ADAPTER_VIEW_WIDTH)

        adapter = MyPhotosAdapter(requireContext(), imageLoader)
        adapter.init()

        val layoutManager = GridLayoutManager(requireContext(), columnsCount)
        layoutManager.spanSizeLookup = MyPhotosAdapterSpanSizeLookup(adapter, columnsCount)

        myPhotosList.layoutManager = layoutManager
        myPhotosList.adapter = adapter
    }

    private fun initRx() {
        compositeDisposable += viewModel.fragmentsLoadPhotosSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .switchMap { viewModel.loadPhotos().toObservable() }
            .doOnNext { photos -> onPhotosLoadedFromDatabase(photos) }
            .subscribe()

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
                    adapter.updatePhotoState(event.myPhoto.id, PhotoState.PHOTO_UPLOADING)
                }
                is PhotoUploadingEvent.OnProgress -> {
                    adapter.updatePhotoProgress(event.photoId, event.progress)
                }
                is PhotoUploadingEvent.OnUploaded -> {
                    adapter.updatePhotoState(event.myPhoto.id, PhotoState.PHOTO_UPLOADED)
                }
                is PhotoUploadingEvent.OnEnd -> {
                }
                is PhotoUploadingEvent.OnFailedToUpload -> {
                    adapter.updatePhotoState(event.myPhoto.id, PhotoState.FAILED_TO_UPLOAD)
                }
                is PhotoUploadingEvent.OnUnknownError -> {
                    adapter.clear()
                }
                else -> throw IllegalArgumentException("Unknown PhotoUploadingEvent $event")
            }
        }
    }

    private fun onPhotosLoadedFromDatabase(uploadedPhotos: List<MyPhoto>) {
        if (!isAdded) {
            return
        }

        requireActivity().runOnUiThread {
            if (uploadedPhotos.isNotEmpty()) {
                val mapped = uploadedPhotos.map { MyPhotosAdapterItem.MyPhotoItem(it) }
                adapter.addAll(mapped)
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
