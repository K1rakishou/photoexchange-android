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

    private var uploadPhotoService: UploadPhotoService? = null

    override fun onServiceConnected(className: ComponentName, _service: IBinder) {
        onUploadingServiceConnected(_service)
    }

    override fun onServiceDisconnected(className: ComponentName) {
        onUploadingServiceDisconnected()
    }

    fun isConnected(): Boolean {
        return this.get()
    }

    private fun onUploadingServiceConnected(_service: IBinder) {
        if (this.compareAndSet(false, true)) {
            Timber.d("+++ onUploadingServiceConnected")

            uploadPhotoService = (_service as UploadPhotoService.UploadPhotosBinder).getService()
            uploadPhotoService!!.attachCallback(WeakReference(activity))
            uploadPhotoService!!.startPhotosUploading()
        } else {
            Timber.d("+++ onUploadingServiceConnected already connected")
        }
    }

    fun onUploadingServiceDisconnected() {
        if (this.compareAndSet(true, false)) {
            Timber.d("--- onUploadingServiceDisconnected")

            uploadPhotoService?.let { srvc ->
                srvc.detachCallback()
                uploadPhotoService = null
            }

            activity.unbindService(this)
        } else {
            Timber.d("--- onUploadingServiceDisconnected already disconnected")
        }
    }
}