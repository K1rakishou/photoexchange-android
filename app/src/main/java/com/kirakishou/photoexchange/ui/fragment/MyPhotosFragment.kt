package com.kirakishou.photoexchange.ui.fragment

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.BindView
import butterknife.ButterKnife
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.model.PhotoUploadingEvent
import com.kirakishou.photoexchange.mvp.model.adapter.AdapterItem
import com.kirakishou.photoexchange.mvp.model.adapter.AdapterItemType
import com.kirakishou.photoexchange.ui.adapter.MyPhotosAdapter
import com.kirakishou.photoexchange.ui.widget.MyPhotosAdapterSpanSizeLookup
import timber.log.Timber


class MyPhotosFragment : Fragment() {

    @BindView(R.id.my_photos_list)
    lateinit var myPhotosList: RecyclerView

    private val PHOTO_ADAPTER_VIEW_WIDTH = 288
    private val imageLoader by lazy { ImageLoader(activity as Context) }

    lateinit var adapter: MyPhotosAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view =  inflater.inflate(R.layout.fragment_my_photos, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerView()
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
                    adapter.add(AdapterItem(event.myPhoto, AdapterItemType.VIEW_MY_PHOTO))
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

    companion object {
        fun newInstance(): MyPhotosFragment {
            val fragment = MyPhotosFragment()
            val args = Bundle()

            fragment.arguments = args
            return fragment
        }
    }
}
