package com.kirakishou.photoexchange.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.module.service.ReceivePhotosServiceModule
import com.kirakishou.photoexchange.helper.Constants
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.event.ReceivedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.callback.ReceivePhotosServiceCallback
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject

class ReceivePhotosService : Service() {

  @Inject
  lateinit var presenter: ReceivePhotosServicePresenter

  private val TAG = "ReceivePhotosService"
  private val compositeDisposable = CompositeDisposable()
  private var notificationManager: NotificationManager? = null
  private val binder = ReceivePhotosBinder()
  private var callback = WeakReference<ReceivePhotosServiceCallback>(null)
  private val NOTIFICATION_ID = 2

  override fun onCreate() {
    super.onCreate()
    resolveDaggerDependency()
    startForeground(NOTIFICATION_ID, createInitialNotification())

    compositeDisposable += presenter.observeResults()
      .subscribe(this::onReceivePhotoResult)
  }

  override fun onDestroy() {
    super.onDestroy()
    removeNotification()
    presenter.onDetach()
    detachCallback()
    compositeDisposable.clear()
  }

  fun attachCallback(_callback: WeakReference<ReceivePhotosServiceCallback>) {
    callback = _callback
  }

  fun detachCallback() {
    callback = WeakReference<ReceivePhotosServiceCallback>(null)
  }

  fun startPhotosReceiving() {
    presenter.startPhotosReceiving()
  }

  private fun onReceivePhotoResult(event: ReceivePhotosServicePresenter.ReceivePhotoEvent) {
    when (event) {
      is ReceivePhotosServicePresenter.ReceivePhotoEvent.OnPhotosReceived -> {
        if (event.receivedPhotos.isNotEmpty()) {
          val photosReceivedEvent = ReceivedPhotosFragmentEvent.ReceivePhotosEvent.PhotosReceived(
            event.receivedPhotos
          )

          callback.get()?.onReceivePhotoEvent(photosReceivedEvent)
        } else {
          Timber.tag(TAG).d("event.receivedPhotos is EMPTY")
        }
      }
      is ReceivePhotosServicePresenter.ReceivePhotoEvent.OnError -> {
        callback.get()?.onReceivePhotoEvent(ReceivedPhotosFragmentEvent.ReceivePhotosEvent
          .OnFailed(event.error))
      }
      is ReceivePhotosServicePresenter.ReceivePhotoEvent.StopService -> {
        stopService()
      }
      is ReceivePhotosServicePresenter.ReceivePhotoEvent.RemoveNotification -> {
        removeNotification()
      }
      is ReceivePhotosServicePresenter.ReceivePhotoEvent.OnNewNotification -> {
        when (event.type) {
          is ReceivePhotosServicePresenter.NotificationType.Progress -> updateNotificationShowProgress()
          is ReceivePhotosServicePresenter.NotificationType.Success -> updateNotificationShowSuccess()
          is ReceivePhotosServicePresenter.NotificationType.Error -> updateNotificationShowError()
        }.safe
      }
    }.safe
  }

  private fun stopService() {
    stopForeground(true)
    stopSelf()
  }

  private fun removeNotification() {
    getNotificationManager().cancel(NOTIFICATION_ID)
  }

  private fun updateNotificationShowProgress() {
    val newNotification = createNotificationDownloading()
    getNotificationManager().notify(NOTIFICATION_ID, newNotification)
  }

  private fun updateNotificationShowSuccess() {
    val newNotification = createNotificationSuccess()
    getNotificationManager().notify(NOTIFICATION_ID, newNotification)
  }

  private fun updateNotificationShowError() {
    val newNotification = createNotificationError()
    getNotificationManager().notify(NOTIFICATION_ID, newNotification)
  }

  private fun createNotificationError(): Notification {
    if (AndroidUtils.isOreoOrHigher()) {
      createNotificationChannelIfNotExists()

      return NotificationCompat.Builder(this, Constants.CHANNEL_ID)
        .setContentTitle("Error")
        .setContentText("Could not receive a photo")
        .setSmallIcon(android.R.drawable.stat_notify_error)
        .setWhen(System.currentTimeMillis())
        .setContentIntent(getNotificationIntent())
        .setAutoCancel(true)
        .build()
    } else {
      return NotificationCompat.Builder(this, Constants.CHANNEL_ID)
        .setContentTitle("Error")
        .setContentText("Could not receive a  photo")
        .setSmallIcon(android.R.drawable.stat_notify_error)
        .setWhen(System.currentTimeMillis())
        .setContentIntent(getNotificationIntent())
        .setAutoCancel(true)
        .build()
    }
  }

  private fun createNotificationSuccess(): Notification {
    if (AndroidUtils.isOreoOrHigher()) {
      createNotificationChannelIfNotExists()

      return NotificationCompat.Builder(this, Constants.CHANNEL_ID)
        .setContentTitle("Done")
        .setContentText("Photo has been found!")
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setWhen(System.currentTimeMillis())
        .setContentIntent(getNotificationIntent())
        .setAutoCancel(true)
        .build()
    } else {
      return NotificationCompat.Builder(this)
        .setContentTitle("Done")
        .setContentText("Photo has been found!")
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setWhen(System.currentTimeMillis())
        .setContentIntent(getNotificationIntent())
        .setAutoCancel(true)
        .build()
    }
  }

  private fun createNotificationDownloading(): Notification {
    if (AndroidUtils.isOreoOrHigher()) {
      createNotificationChannelIfNotExists()

      return NotificationCompat.Builder(this, Constants.CHANNEL_ID)
        .setContentTitle("Please wait")
        .setContentText("Looking for photo answer...")
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setWhen(System.currentTimeMillis())
        .setContentIntent(getNotificationIntent())
        .setAutoCancel(true)
        .setOngoing(true)
        .build()
    } else {
      return NotificationCompat.Builder(this)
        .setContentTitle("Please wait")
        .setContentText("Looking for photo answer...")
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setWhen(System.currentTimeMillis())
        .setContentIntent(getNotificationIntent())
        .setAutoCancel(false)
        .setOngoing(true)
        .build()
    }
  }

  private fun createInitialNotification(): Notification {
    if (AndroidUtils.isOreoOrHigher()) {
      createNotificationChannelIfNotExists()

      return NotificationCompat.Builder(this, Constants.CHANNEL_ID)
        .setContentTitle("Please wait")
        .setContentText("Looking for photo answer...")
        .setWhen(System.currentTimeMillis())
        .setContentIntent(getNotificationIntent())
        .setAutoCancel(true)
        .setOngoing(true)
        .build()
    } else {
      return NotificationCompat.Builder(this)
        .setContentTitle("Please wait")
        .setContentText("Looking for photo answer...")
        .setWhen(System.currentTimeMillis())
        .setContentIntent(getNotificationIntent())
        .setAutoCancel(true)
        .setOngoing(true)
        .build()
    }
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun createNotificationChannelIfNotExists() {
    if (getNotificationManager().getNotificationChannel(Constants.CHANNEL_ID) == null) {
      val notificationChannel = NotificationChannel(
        Constants.CHANNEL_ID,
        Constants.CHANNEL_NAME,
        NotificationManager.IMPORTANCE_LOW
      )

      notificationChannel.enableLights(false)
      notificationChannel.enableVibration(false)
      notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

      getNotificationManager().createNotificationChannel(notificationChannel)
    }
  }

  private fun getNotificationIntent(): PendingIntent {
    val notificationIntent = Intent(this, PhotosActivity::class.java)
    notificationIntent.action = Intent.ACTION_MAIN
    notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER)

    return PendingIntent.getActivity(
      this,
      0,
      notificationIntent,
      PendingIntent.FLAG_UPDATE_CURRENT)
  }

  private fun getNotificationManager(): NotificationManager {
    if (notificationManager == null) {
      notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    return notificationManager!!
  }

  private fun resolveDaggerDependency() {
    (application as PhotoExchangeApplication).applicationComponent
      .plus(ReceivePhotosServiceModule())
      .inject(this)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    return START_NOT_STICKY
  }

  override fun onBind(intent: Intent?): IBinder {
    return binder
  }

  inner class ReceivePhotosBinder : Binder() {
    fun getService(): ReceivePhotosService {
      return this@ReceivePhotosService
    }
  }
}