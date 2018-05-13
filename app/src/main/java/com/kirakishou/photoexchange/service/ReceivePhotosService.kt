package com.kirakishou.photoexchange.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.module.ReceivePhotosServiceModule
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.ReceivePhotosEvent
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.callback.ReceivePhotosServiceCallback
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject
import android.app.ActivityManager




class ReceivePhotosService : Service(), ReceivePhotosServiceCallbacks {

    @Inject
    lateinit var presenter: ReceivePhotosServicePresenter

    private val tag = "ReceivePhotosService"
    private val compositeDisposable = CompositeDisposable()
    private var notificationManager: NotificationManager? = null
    private val binder = ReceivePhotosBinder()
    private var callback = WeakReference<ReceivePhotosServiceCallback>(null)
    private val NOTIFICATION_ID = 2
    private val CHANNEL_ID = "1"
    private val CHANNED_NAME = "name"

    override fun onCreate() {
        super.onCreate()
        Timber.tag(tag).d("ReceivePhotosService started")

        resolveDaggerDependency()
        startForeground(NOTIFICATION_ID, createNotificationDownloading())
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.tag(tag).d("ReceivePhotosService destroyed")

        presenter.onDetach()
        compositeDisposable.clear()
    }

    fun attachCallback(_callback: WeakReference<ReceivePhotosServiceCallback>) {
        callback = _callback
    }

    fun detachCallback() {
        callback = WeakReference<ReceivePhotosServiceCallback>(null)
    }

    fun startPhotosReceiving() {
        updateUploadingNotificationShowDownloading()
        presenter.startPhotosReceiving()
    }

    override fun onPhotoReceived(receivedPhoto: ReceivedPhoto, takenPhotoId: Long) {
        updateDownloadingNotificationShowSuccess()
        callback.get()?.onPhotoFindEvent(ReceivePhotosEvent.OnPhotoReceived(receivedPhoto, takenPhotoId))
    }

    override fun onFailed(errorCode: ErrorCode.ReceivePhotosErrors) {
        updateUploadingNotificationShowError()
        callback.get()?.onPhotoFindEvent(ReceivePhotosEvent.OnFailed(errorCode))
    }

    override fun onError(error: Throwable) {
        updateUploadingNotificationShowError()
        callback.get()?.onPhotoFindEvent(ReceivePhotosEvent.OnUnknownError(error))
    }

    override fun stopService() {
        Timber.tag(tag).d("Stopping service")

        stopForeground(true)
        stopSelf()
    }

    private fun updateUploadingNotificationShowDownloading() {
        val newNotification = createNotificationDownloading()
        getNotificationManager().notify(NOTIFICATION_ID, newNotification)
    }

    private fun updateDownloadingNotificationShowSuccess() {
        val newNotification = createNotificationSuccess()
        getNotificationManager().notify(NOTIFICATION_ID, newNotification)
    }

    private fun updateUploadingNotificationShowError() {
        val newNotification = createNotificationError()
        getNotificationManager().notify(NOTIFICATION_ID, newNotification)
    }

    private fun createNotificationError(): Notification {
        if (AndroidUtils.isOreoOrHigher()) {
            createNotificationChannelIfNotExists()

            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Error")
                .setContentText("Could not upload photo")
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(getNotificationIntent())
                .setAutoCancel(true)
                .build()
        } else {
            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Error")
                .setContentText("Could not upload photo")
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

            return NotificationCompat.Builder(this, CHANNEL_ID)
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

            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Please wait")
                .setContentText("Looking for photo answer...")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(getNotificationIntent())
                .setAutoCancel(false)
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannelIfNotExists() {
        if (getNotificationManager().getNotificationChannel(CHANNEL_ID) == null) {
            val notificationChannel = NotificationChannel(CHANNEL_ID, CHANNED_NAME,
                NotificationManager.IMPORTANCE_LOW)

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
        //notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

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
            .plus(ReceivePhotosServiceModule(this))
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

    companion object {
        fun isRunning(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
            for (service in manager!!.getRunningServices(Integer.MAX_VALUE)) {
                if (ReceivePhotosService::class.java.name == service.service.className) {
                    return true
                }
            }
            return false
        }
    }
}