package com.kirakishou.photoexchange.service

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import com.kirakishou.photoexchange.ui.activity.AllPhotosActivity
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

class UploadPhotoServiceConnection(
    val activity: AllPhotosActivity
) : AtomicBoolean(false), ServiceConnection {

    private val tag = "UploadPhotoServiceConnection"
    private var uploadPhotoService: UploadPhotoService? = null

    override fun onServiceConnected(className: ComponentName, _service: IBinder) {
        onUploadingServiceConnected(_service)
    }

    override fun onServiceDisconnected(className: ComponentName) {
        onUploadingServiceDisconnected()
    }

    private fun onUploadingServiceConnected(_service: IBinder) {
        if (this.compareAndSet(false, true)) {
            Timber.tag(tag).d("+++ onUploadingServiceConnected")

            uploadPhotoService = (_service as UploadPhotoService.UploadPhotosBinder).getService()
            uploadPhotoService!!.attachCallback(WeakReference(activity))
            uploadPhotoService!!.startPhotosUploading()
        } else {
            Timber.tag(tag).d("+++ onUploadingServiceConnected already connected")
        }
    }

    fun onUploadingServiceDisconnected() {
        if (this.compareAndSet(true, false)) {
            Timber.tag(tag).d("--- onUploadingServiceDisconnected")

            uploadPhotoService?.let { srvc ->
                srvc.detachCallback()
                uploadPhotoService = null
            }

            activity.unbindService(this)
        } else {
            Timber.tag(tag).d("--- onUploadingServiceDisconnected already disconnected")
        }
    }
}