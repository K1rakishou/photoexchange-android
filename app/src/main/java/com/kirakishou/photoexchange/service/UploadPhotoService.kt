package com.kirakishou.photoexchange.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.module.UploadPhotoServiceModule
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.location.LocationService
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.callback.PhotoUploadingServiceCallback
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Created by kirakishou on 3/11/2018.
 */
class UploadPhotoService : Service(), CoroutineScope {

  @Inject
  lateinit var presenter: UploadPhotoServicePresenter

  @Inject
  lateinit var locationService: LocationService

  @Inject
  lateinit var dispatchersProvider: DispatchersProvider

  private val TAG = "UploadPhotoService"

  private var notificationManager: NotificationManager? = null
  private val binder = UploadPhotosBinder()
  private val compositeDisposable = CompositeDisposable()

  private var callback = WeakReference<PhotoUploadingServiceCallback>(null)
  private val NOTIFICATION_ID = 1
  private val CHANNEL_ID by lazy { getString(R.string.default_notification_channel_id) }
  private val CHANNED_NAME = "name"

  lateinit var job: Job

  override val coroutineContext: CoroutineContext
    get() = job + dispatchersProvider.UI()

  override fun onCreate() {
    super.onCreate()
    job = Job()

    resolveDaggerDependency()
    startForeground(NOTIFICATION_ID, createInitialNotification())

    compositeDisposable += presenter.observeResults()
      .subscribe(this::onUploadingPhotoResult)
  }

  override fun onDestroy() {
    super.onDestroy()

    removeNotification()
    presenter.onDetach()
    detachCallback()

    job.cancel()
    compositeDisposable.clear()
  }

  fun attachCallback(_callback: WeakReference<PhotoUploadingServiceCallback>) {
    callback = _callback
  }

  fun detachCallback() {
    callback = WeakReference<PhotoUploadingServiceCallback>(null)
  }

  fun startPhotosUploading() {
    requireNotNull(callback.get())

    launch {
      val location = locationService.getCurrentLocation()
      presenter.uploadPhotos(location)
    }
  }

  fun cancelPhotoUploading(photoId: Long) {
    presenter.cancelPhotoUploading(photoId)
  }

  private fun onUploadingPhotoResult(event: UploadPhotoServicePresenter.UploadPhotoEvent) {
    when (event) {
      is UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent -> {
        callback.get()?.onUploadPhotosEvent(event.nestedEvent)
      }
      is UploadPhotoServicePresenter.UploadPhotoEvent.RemoveNotification -> {
        removeNotification()
      }
      is UploadPhotoServicePresenter.UploadPhotoEvent.StopService -> {
        stopService()
      }
      is UploadPhotoServicePresenter.UploadPhotoEvent.OnNewNotification -> {
        when (event.type) {
          is UploadPhotoServicePresenter.NotificationType.Uploading -> updateNotificationShowProgress()
          is UploadPhotoServicePresenter.NotificationType.Success -> updateNotificationShowSuccess(event.type.message)
          is UploadPhotoServicePresenter.NotificationType.Error -> updateNotificationShowError(event.type.errorMessage)
        }.safe
      }
    }.safe
  }

  private fun stopService() {
    Timber.tag(TAG).d("Stopping service")

    stopForeground(true)
    stopSelf()
  }

  //notifications
  private fun removeNotification() {
    getNotificationManager().cancel(NOTIFICATION_ID)
  }

  private fun updateNotificationShowProgress() {
    val newNotification = createNotificationUploading()
    getNotificationManager().notify(NOTIFICATION_ID, newNotification)
  }

  private fun updateNotificationShowSuccess(message: String) {
    val newNotification = createNotificationSuccess(message)
    getNotificationManager().notify(NOTIFICATION_ID, newNotification)
  }

  private fun updateNotificationShowError(message: String) {
    val newNotification = createNotificationError(message)
    getNotificationManager().notify(NOTIFICATION_ID, newNotification)
  }

  private fun createNotificationError(message: String): Notification {
    if (AndroidUtils.isOreoOrHigher()) {
      createNotificationChannelIfNotExists()

      return NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Error")
        .setContentText(message)
        .setSmallIcon(android.R.drawable.stat_notify_error)
        .setWhen(System.currentTimeMillis())
        .setContentIntent(getNotificationIntent())
        .setAutoCancel(true)
        .build()
    } else {
      return NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Error")
        .setContentText(message)
        .setSmallIcon(android.R.drawable.stat_notify_error)
        .setWhen(System.currentTimeMillis())
        .setContentIntent(getNotificationIntent())
        .setAutoCancel(true)
        .build()
    }
  }

  private fun createNotificationSuccess(message: String): Notification {
    if (AndroidUtils.isOreoOrHigher()) {
      createNotificationChannelIfNotExists()

      return NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Done")
        .setContentText(message)
        .setSmallIcon(android.R.drawable.stat_sys_upload_done)
        .setWhen(System.currentTimeMillis())
        .setContentIntent(getNotificationIntent())
        .setAutoCancel(true)
        .build()
    } else {
      return NotificationCompat.Builder(this)
        .setContentTitle("Done")
        .setContentText(message)
        .setSmallIcon(android.R.drawable.stat_sys_upload_done)
        .setWhen(System.currentTimeMillis())
        .setContentIntent(getNotificationIntent())
        .setAutoCancel(true)
        .build()
    }
  }

  private fun createNotificationUploading(): Notification {
    if (AndroidUtils.isOreoOrHigher()) {
      createNotificationChannelIfNotExists()

      return NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Please wait")
        .setContentText("Uploading photo...")
        .setSmallIcon(android.R.drawable.stat_sys_upload)
        .setWhen(System.currentTimeMillis())
        .setContentIntent(getNotificationIntent())
        .setAutoCancel(true)
        .setOngoing(true)
        .build()
    } else {
      return NotificationCompat.Builder(this)
        .setContentTitle("Please wait")
        .setContentText("Uploading photo...")
        .setSmallIcon(android.R.drawable.stat_sys_upload)
        .setWhen(System.currentTimeMillis())
        .setContentIntent(getNotificationIntent())
        .setAutoCancel(true)
        .setOngoing(true)
        .build()
    }
  }

  private fun createInitialNotification(): Notification {
    if (AndroidUtils.isOreoOrHigher()) {
      createNotificationChannelIfNotExists()

      return NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Please wait")
        .setContentText("Uploading photo...")
        .setWhen(System.currentTimeMillis())
        .setContentIntent(getNotificationIntent())
        .setAutoCancel(true)
        .setOngoing(true)
        .build()
    } else {
      return NotificationCompat.Builder(this)
        .setContentTitle("Please wait")
        .setContentText("Uploading photo...")
        .setWhen(System.currentTimeMillis())
        .setContentIntent(getNotificationIntent())
        .setAutoCancel(true)
        .setOngoing(true)
        .build()
    }
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun createNotificationChannelIfNotExists() {
    if (getNotificationManager().getNotificationChannel(CHANNEL_ID) == null) {
      val notificationChannel = NotificationChannel(
        CHANNEL_ID,
        CHANNED_NAME,
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
      .plus(UploadPhotoServiceModule())
      .inject(this)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    return START_NOT_STICKY
  }

  override fun onBind(intent: Intent?): IBinder {
    return binder
  }

  inner class UploadPhotosBinder : Binder() {
    fun getService(): UploadPhotoService {
      return this@UploadPhotoService
    }
  }
}