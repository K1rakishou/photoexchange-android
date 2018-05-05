package com.kirakishou.photoexchange.service

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import com.kirakishou.photoexchange.ui.activity.AllPhotosActivity
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

class FindPhotoAnswerServiceConnection(
    val activity: AllPhotosActivity
) : AtomicBoolean(false), ServiceConnection {

    private var findPhotoAnswerService: FindPhotoAnswerService? = null

    override fun onServiceConnected(className: ComponentName, _service: IBinder) {
        onFindingServiceConnected(_service)
    }

    override fun onServiceDisconnected(className: ComponentName) {
        onFindingServiceDisconnected()
    }

    fun isConnected(): Boolean {
        return this.get()
    }

    fun startSearchingForPhotoAnswers() {
        findPhotoAnswerService!!.startSearchingForPhotoAnswers()
    }

    private fun onFindingServiceConnected(_service: IBinder) {
        if (this.compareAndSet(false, true)) {
            findPhotoAnswerService = (_service as FindPhotoAnswerService.FindPhotoAnswerBinder).getService()
            findPhotoAnswerService!!.attachCallback(WeakReference(activity))
            startSearchingForPhotoAnswers()
        }
    }

    fun onFindingServiceDisconnected() {
        if (this.compareAndSet(true, false)) {
            findPhotoAnswerService?.let { srvc ->
                srvc.detachCallback()
                findPhotoAnswerService = null
            }

            activity.unbindService(this)
        }
    }
}