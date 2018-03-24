package com.kirakishou.photoexchange.ui.fragment

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseFragment
import com.kirakishou.photoexchange.di.module.AllPhotosActivityModule
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoUploadingEvent
import com.kirakishou.photoexchange.mvp.model.adapter.AdapterItem
import com.kirakishou.photoexchange.mvp.model.adapter.AdapterItemType
import com.kirakishou.photoexchange.mvp.viewmodel.AllPhotosActivityViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.factory.AllPhotosActivityViewModelFactory
import com.kirakishou.photoexchange.ui.activity.AllPhotosActivity
import com.kirakishou.photoexchange.ui.adapter.MyPhotosAdapter
import com.kirakishou.photoexchange.ui.widget.MyPhotosAdapterSpanSizeLookup
import timber.log.Timber
import javax.inject.Inject


class MyPhotosFragment : BaseFragment<AllPhotosActivityViewModel>() {

    @BindView(R.id.my_photos_list)
    lateinit var myPhotosList: RecyclerView

    @Inject
    lateinit var viewModelFactory: AllPhotosActivityViewModelFactory

    @Inject
    lateinit var imageLoader: ImageLoader

    private val PHOTO_ADAPTER_VIEW_WIDTH = 288
    lateinit var adapter: MyPhotosAdapter

    override fun initViewModel(): AllPhotosActivityViewModel? {
        return ViewModelProviders.of(this, viewModelFactory).get(AllPhotosActivityViewModel::class.java)
    }

    override fun getContentView(): Int = R.layout.fragment_my_photos

    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
        initRecyclerView()
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

    fun onUploadedPhotosLoadedFromDatabase(uploadedPhotos: List<MyPhoto>) {
        if (!isAdded) {
            return
        }

        adapter.runOnAdapterHandler {
            val mapped = uploadedPhotos.map { AdapterItem(it, AdapterItemType.VIEW_MY_PHOTO) }
            adapter.addAll(mapped)
        }
    }

    override fun resolveDaggerDependency() {
        (activity!!.application as PhotoExchangeApplication).applicationComponent
            .plus(AllPhotosActivityModule(activity as AllPhotosActivity))
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
