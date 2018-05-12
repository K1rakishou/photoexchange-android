package com.kirakishou.photoexchange.service

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

class ReceivePhotosServiceConnection(
    val activity: PhotosActivity
) : AtomicBoolean(false), ServiceConnection {

    private var receivePhotosService: ReceivePhotosService? = null

    override fun onServiceConnected(className: ComponentName, _service: IBinder) {
        onFindingServiceConnected(_service)
    }

    override fun onServiceDisconnected(className: ComponentName) {
        onFindingServiceDisconnected()
    }

    fun isConnected(): Boolean {
        return this.get()
    }

    fun startPhotosReceiving() {
        receivePhotosService!!.startPhotosReceiving()
    }

    private fun onFindingServiceConnected(_service: IBinder) {
        if (this.compareAndSet(false, true)) {
            receivePhotosService = (_service as ReceivePhotosService.ReceivePhotosBinder).getService()
            receivePhotosService!!.attachCallback(WeakReference(activity))
            startPhotosReceiving()
        }
    }

    fun onFindingServiceDisconnected() {
        if (this.compareAndSet(true, false)) {
            receivePhotosService?.let { srvc ->
                srvc.detachCallback()
                receivePhotosService = null
            }

            activity.unbindService(this)
        }
    }
}