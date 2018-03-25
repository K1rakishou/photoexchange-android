package com.kirakishou.photoexchange.ui.fragment

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.base.BaseFragment
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoUploadingEvent
import com.kirakishou.photoexchange.mvp.model.adapter.AdapterItem
import com.kirakishou.photoexchange.mvp.model.adapter.AdapterItemType
import com.kirakishou.photoexchange.mvp.viewmodel.AllPhotosActivityViewModel
import com.kirakishou.photoexchange.ui.activity.AllPhotosActivity
import com.kirakishou.photoexchange.ui.adapter.MyPhotosAdapter
import com.kirakishou.photoexchange.ui.widget.MyPhotosAdapterSpanSizeLookup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import timber.log.Timber
import javax.inject.Inject


class MyPhotosFragment : BaseFragment() {

    @BindView(R.id.my_photos_list)
    lateinit var myPhotosList: RecyclerView

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var coroutinesPool: CoroutineThreadPoolProvider

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

    private fun onUploadingEvent(event: PhotoUploadingEvent) {
        activity?.runOnUiThread {
            when (event) {
                is PhotoUploadingEvent.OnPrepare -> {
                    Timber.e("OnPrepare")
                }
                is PhotoUploadingEvent.OnPhotoUploadingStart -> {
                    Timber.e("OnPhotoUploadingStart")
                    myPhotosList.scrollToPosition(0)
                    adapter.add(0, AdapterItem(event.myPhoto, AdapterItemType.VIEW_MY_PHOTO))
                }
                is PhotoUploadingEvent.OnProgress -> {
                    Timber.e("OnProgress ${event.progress}")
                    adapter.updatePhotoProgress(event.photoId, event.progress)
                }
                is PhotoUploadingEvent.OnUploaded -> {
                    Timber.e("OnUploaded")
                    adapter.updatePhotoState(event.myPhoto.id, event.myPhoto.photoState)
                }
                is PhotoUploadingEvent.OnEnd -> {
                    Timber.e("OnEnd")
                }
                is PhotoUploadingEvent.OnFailedToUpload -> {
                    Timber.e("OnFailedToUpload")
                }
                is PhotoUploadingEvent.OnUnknownError -> {
                    Timber.e("OnUnknownError")
                }
            }
        }
    }

    private fun onUploadedPhotosLoadedFromDatabase(uploadedPhotos: List<MyPhoto>) {
        activity?.runOnUiThread {
            val mapped = uploadedPhotos.map { AdapterItem(it, AdapterItemType.VIEW_MY_PHOTO) }
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
