package com.kirakishou.photoexchange.ui.fragment

import android.content.Context
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.base.BaseFragment
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoUploadingEvent
import com.kirakishou.photoexchange.mvp.model.adapter.AdapterItem
import com.kirakishou.photoexchange.mvp.model.adapter.AdapterItemType
import com.kirakishou.photoexchange.mvp.viewmodel.AllPhotosActivityViewModel
import com.kirakishou.photoexchange.ui.activity.AllPhotosActivity
import com.kirakishou.photoexchange.ui.adapter.MyPhotosAdapter
import com.kirakishou.photoexchange.ui.widget.MyPhotosAdapterSpanSizeLookup
import timber.log.Timber


class MyPhotosFragment : BaseFragment<AllPhotosActivityViewModel>() {

    @BindView(R.id.my_photos_list)
    lateinit var myPhotosList: RecyclerView

    private val PHOTO_ADAPTER_VIEW_WIDTH = 288
    private val imageLoader by lazy { ImageLoader(activity as Context) }

    lateinit var adapter: MyPhotosAdapter

    override fun initViewModel(): AllPhotosActivityViewModel {
        return (activity as AllPhotosActivity).getViewModel()
    }

    override fun getContentView(): Int = R.layout.fragment_my_photos

    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
        initRecyclerView()

        getViewModel().loadUploadedPhotos()
    }

    override fun onFragmentViewDestroy() {
    }

    private fun initRecyclerView() {
        val columnsCount = AndroidUtils.calculateNoOfColumns(activity!!, PHOTO_ADAPTER_VIEW_WIDTH)

        adapter = MyPhotosAdapter(activity!!, imageLoader)
        adapter.init()

        val layoutManager = GridLayoutManager(activity, columnsCount)
        layoutManager.spanSizeLookup = MyPhotosAdapterSpanSizeLookup(adapter, columnsCount)

        myPhotosList.layoutManager = layoutManager
        myPhotosList.adapter = adapter
        myPhotosList.setHasFixedSize(true)
    }

    fun onUploadingEvent(event: PhotoUploadingEvent) {
        if (!isAdded) {
            return
        }

        adapter.runOnAdapterHandler {
            when (event) {
                is PhotoUploadingEvent.onPrepare -> {
                    Timber.e("onPrepare")
                }
                is PhotoUploadingEvent.onPhotoUploadingStart -> {
                    Timber.e("onPhotoUploadingStart")
                    myPhotosList.scrollToPosition(0)
                    adapter.add(0, AdapterItem(event.myPhoto, AdapterItemType.VIEW_MY_PHOTO))
                }
                is PhotoUploadingEvent.onProgress -> {
                    Timber.e("onProgress ${event.progress}")
                    adapter.updatePhotoProgress(event.photoId, event.progress)
                }
                is PhotoUploadingEvent.onUploaded -> {
                    Timber.e("onUploaded")
                    adapter.updatePhotoState(event.myPhoto.id, event.myPhoto.photoState)
                }
                is PhotoUploadingEvent.onFailedToUpload -> {
                    Timber.e("onFailedToUpload")
                }
                is PhotoUploadingEvent.onUnknownError -> {
                    Timber.e("onUnknownError")
                }
                is PhotoUploadingEvent.onEnd -> {
                    Timber.e("onEnd")
                }
            }
        }
    }

    fun onUploadedPhotos(uploadedPhotos: List<MyPhoto>) {
        if (!isAdded) {
            return
        }

        adapter.runOnAdapterHandler {
            val mapped = uploadedPhotos.map { AdapterItem(it, AdapterItemType.VIEW_MY_PHOTO) }
            adapter.addAll(mapped)
        }
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
