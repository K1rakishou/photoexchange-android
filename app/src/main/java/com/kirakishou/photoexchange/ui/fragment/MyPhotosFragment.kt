package com.kirakishou.photoexchange.ui.fragment

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoUploadingEvent
import com.kirakishou.photoexchange.mvp.viewmodel.AllPhotosActivityViewModel
import com.kirakishou.photoexchange.ui.activity.AllPhotosActivity
import com.kirakishou.photoexchange.ui.adapter.MyPhotosAdapter
import com.kirakishou.photoexchange.ui.adapter.MyPhotosAdapterItem
import com.kirakishou.photoexchange.ui.viewstate.MyPhotosFragmentViewState
import com.kirakishou.photoexchange.ui.adapter.MyPhotosAdapterSpanSizeLookup
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

    private val PHOTO_ADAPTER_VIEW_WIDTH = 288
    lateinit var adapter: MyPhotosAdapter

    override fun getContentView(): Int = R.layout.fragment_my_photos

    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
        initRx()
        initRecyclerView()
        showUploadedPhotos()
    }

    override fun onFragmentViewDestroy() {
    }

    private fun showUploadedPhotos() {
        compositeDisposable += viewModel.loadUploadedPhotosFromDatabase()
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { photos -> onUploadedPhotosLoadedFromDatabase(photos) }
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
    }


    private fun initRecyclerView() {
        val columnsCount = AndroidUtils.calculateNoOfColumns(activity!!, PHOTO_ADAPTER_VIEW_WIDTH)

        adapter = MyPhotosAdapter(activity!!, imageLoader)
        adapter.init()

        val layoutManager = GridLayoutManager(activity!!, columnsCount)
        layoutManager.spanSizeLookup = MyPhotosAdapterSpanSizeLookup(adapter, columnsCount)

        myPhotosList.layoutManager = layoutManager
        myPhotosList.adapter = adapter
    }

    private fun onViewStateChanged(viewState: MyPhotosFragmentViewState) {
        if (!isAdded) {
            return
        }

        activity?.runOnUiThread {
            when (viewState) {
                is MyPhotosFragmentViewState.Default -> {

                }
                is MyPhotosFragmentViewState.ShowObtainCurrentLocationNotification -> {
                    if (viewState.show) {
                        adapter.showObtainCurrentLocationNotification()
                    } else {
                        adapter.hideObtainCurrentLocationNotification()
                    }
                }
            }
        }
    }

    private fun onUploadingEvent(event: PhotoUploadingEvent) {
        if (!isAdded) {
            return
        }

        activity?.runOnUiThread {
            when (event) {
                is PhotoUploadingEvent.OnPrepare -> {
                    myPhotosList.scrollToPosition(0)
                }
                is PhotoUploadingEvent.OnPhotoUploadingStart -> {
                    adapter.updateQueuedUpPhotosCountNotification(event.queuedUpPhotosCount)
                    adapter.add(0, MyPhotosAdapterItem.MyPhotoItem(event.myPhoto))
                }
                is PhotoUploadingEvent.OnProgress -> {
                    adapter.updatePhotoProgress(event.photoId, event.progress)
                }
                is PhotoUploadingEvent.OnUploaded -> {
                    adapter.updatePhotoState(event.myPhoto.id, event.myPhoto.photoState)
                }
                is PhotoUploadingEvent.OnEnd -> {
                    adapter.hideQueuedUpPhotosCountNotification()
                }
                is PhotoUploadingEvent.OnFailedToUpload -> {
                }
                is PhotoUploadingEvent.OnUnknownError -> {
                }
            }
        }
    }

    private fun onUploadedPhotosLoadedFromDatabase(uploadedPhotos: List<MyPhoto>) {
        activity?.runOnUiThread {
            val mapped = uploadedPhotos.map { MyPhotosAdapterItem.MyPhotoItem(it) }
            adapter.addAll(mapped)
        }
    }

    override fun resolveDaggerDependency() {
        (activity as AllPhotosActivity).activityComponent
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
