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
import com.kirakishou.photoexchange.di.module.FindPhotoAnswerServiceModule
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.model.PhotoAnswer
import com.kirakishou.photoexchange.mvp.model.PhotoFindEvent
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.ui.activity.AllPhotosActivity
import com.kirakishou.photoexchange.ui.callback.FindPhotoAnswerCallback
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject


class FindPhotoAnswerService : Service(), FindPhotoAnswerServiceCallbacks {

    @Inject
    lateinit var presenter: FindPhotoAnswerServicePresenter

    private val tag = "[${this::class.java.simpleName}] "
    private val compositeDisposable = CompositeDisposable()
    private var notificationManager: NotificationManager? = null
    private val binder = FindPhotoAnswerBinder()
    private var callback = WeakReference<FindPhotoAnswerCallback>(null)
    private val NOTIFICATION_ID = 2
    private val CHANNEL_ID = "1"
    private val CHANNED_NAME = "name"

    override fun onCreate() {
        super.onCreate()
        Timber.tag(tag).d("Service started")

        resolveDaggerDependency()
        startForeground(NOTIFICATION_ID, createNotificationDownloading())
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.tag(tag).d("Service destroyed")

        presenter.onDetach()
        compositeDisposable.clear()
    }

    fun attachCallback(_callback: WeakReference<FindPhotoAnswerCallback>) {
        callback = _callback
    }

    fun detachCallback() {
        callback = WeakReference<FindPhotoAnswerCallback>(null)
    }

    fun startSearchingForPhotoAnswers() {
        updateUploadingNotificationShowDownloading()
        presenter.startFindPhotoAnswers()
    }

    override fun onPhotoReceived(photoAnswer: PhotoAnswer) {
        updateDownloadingNotificationShowSuccess()
        callback.get()?.onPhotoFindEvent(PhotoFindEvent.OnPhotoAnswerFound(photoAnswer))
    }

    override fun onFailed(errorCode: ErrorCode.FindPhotoAnswerErrors) {
        updateUploadingNotificationShowError()
        callback.get()?.onPhotoFindEvent(PhotoFindEvent.OnFailed(errorCode))
    }

    override fun onError(error: Throwable) {
        updateUploadingNotificationShowError()
        callback.get()?.onPhotoFindEvent(PhotoFindEvent.OnUnknownError(error))
    }

    override fun stopService() {
        Timber.e("Stopping service")

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
        val notificationIntent = Intent(this, AllPhotosActivity::class.java)
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
            .plus(FindPhotoAnswerServiceModule(this))
            .inject(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    inner class FindPhotoAnswerBinder : Binder() {
        fun getService(): FindPhotoAnswerService {
            return this@FindPhotoAnswerService
        }
    }
}