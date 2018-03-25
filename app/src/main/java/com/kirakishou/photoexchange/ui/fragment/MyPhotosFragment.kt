package com.kirakishou.photoexchange.ui.fragment

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
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
import com.kirakishou.photoexchange.service.UploadPhotoService
import com.kirakishou.photoexchange.ui.activity.AllPhotosActivity
import com.kirakishou.photoexchange.ui.adapter.MyPhotosAdapter
import com.kirakishou.photoexchange.ui.callback.PhotoUploadingCallback
import com.kirakishou.photoexchange.ui.widget.MyPhotosAdapterSpanSizeLookup
import kotlinx.coroutines.experimental.async
import timber.log.Timber
import javax.inject.Inject


class MyPhotosFragment : BaseFragment(), PhotoUploadingCallback {

    @BindView(R.id.my_photos_list)
    lateinit var myPhotosList: RecyclerView

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var coroutinesPool: CoroutineThreadPoolProvider

    @Inject
    lateinit var viewModel: AllPhotosActivityViewModel

    private val PHOTO_ADAPTER_VIEW_WIDTH = 288
    private var service: UploadPhotoService? = null
    lateinit var adapter: MyPhotosAdapter

    override fun getContentView(): Int = R.layout.fragment_my_photos

    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
        initRecyclerView()
        initChannels()

        viewModel.loadUploadedPhotosFromDatabase()
    }

    override fun onFragmentViewDestroy() {
        service?.let { srvc ->
            srvc.detachCallback()
            activity!!.unbindService(connection)
            service = null
        }
    }

    private fun initChannels() {
        async(coroutinesPool.BG()) {
            while (!viewModel.startUploadingServiceChannel.isClosedForReceive) {
                viewModel.startUploadingServiceChannel.receive()
                startUploadingService()
            }
        }

        async(coroutinesPool.BG()) {
            while (!viewModel.uploadedPhotosChannel.isClosedForReceive) {
                val photos = viewModel.uploadedPhotosChannel.receive()
                onUploadedPhotosLoadedFromDatabase(photos)
            }
        }
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

    override fun onUploadingEvent(event: PhotoUploadingEvent) {
        adapter.runOnAdapterHandler {
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

    private fun startUploadingService() {
        val serviceIntent = Intent(activity, UploadPhotoService::class.java)
        activity!!.startService(serviceIntent)
        activity!!.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun onUploadedPhotosLoadedFromDatabase(uploadedPhotos: List<MyPhoto>) {
        adapter.runOnAdapterHandler {
            val mapped = uploadedPhotos.map { AdapterItem(it, AdapterItemType.VIEW_MY_PHOTO) }
            adapter.clear()
            adapter.addAll(mapped)
        }
    }

    override fun resolveDaggerDependency() {
        (activity as AllPhotosActivity).activityComponent
            .inject(this)
    }

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, _service: IBinder) {
            Timber.tag(tag).d("Service connected")

            service = (_service as UploadPhotoService.UploadPhotosBinder).getService()
            service?.attachCallback(this@MyPhotosFragment)
            service?.startPhotosUploading()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            Timber.tag(tag).d("Service disconnected")

            service?.detachCallback()
            activity!!.unbindService(this)
            service = null
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
