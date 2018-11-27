package com.kirakishou.photoexchange.service

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

class UploadPhotoServiceConnection(
  val activity: PhotosActivity
) : AtomicBoolean(false), ServiceConnection {

  private val tag = "UploadPhotoServiceConnection"
  private var uploadPhotoService: UploadPhotoService? = null

  override fun onServiceConnected(className: ComponentName, _service: IBinder) {
    onUploadingServiceConnected(_service)
  }

  override fun onServiceDisconnected(className: ComponentName) {
    onUploadingServiceDisconnected()
  }

  fun isConnected(): Boolean {
    return get()
  }

  fun startPhotosUploading() {
    if (isConnected()) {
      uploadPhotoService?.startPhotosUploading()
    }
  }

  fun cancelPhotoUploading(photoId: Long) {
    if (isConnected()) {
      uploadPhotoService?.cancelPhotoUploading(photoId)
    }
  }

  private fun onUploadingServiceConnected(_service: IBinder) {
    if (compareAndSet(false, true)) {
      uploadPhotoService = (_service as UploadPhotoService.UploadPhotosBinder).getService()
      uploadPhotoService!!.attachCallback(WeakReference(activity))
      startPhotosUploading()
    }
  }

  fun onUploadingServiceDisconnected() {
    if (compareAndSet(true, false)) {
      uploadPhotoService?.detachCallback()
      activity.unbindService(this)
      uploadPhotoService = null
    }
  }
}