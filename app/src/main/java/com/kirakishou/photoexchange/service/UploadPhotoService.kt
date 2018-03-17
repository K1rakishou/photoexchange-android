package com.kirakishou.photoexchange.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import com.kirakishou.photoexchange.di.component.DaggerUploadPhotoServiceComponent
import com.kirakishou.photoexchange.di.module.*
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.model.PhotoUploadingEvent
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import com.kirakishou.photoexchange.ui.activity.AllPhotosActivity
import com.kirakishou.photoexchange.ui.callback.ActivityCallback
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by kirakishou on 3/11/2018.
 */
class UploadPhotoService : Service(), UploadPhotoServiceCallbacks {

    @Inject
    lateinit var presenter: UploadPhotoServicePresenter

    private val tag = "[${this::class.java.simpleName}] "

    private var notificationManager: NotificationManager? = null
    private val binder = UploadPhotosBinder()

    private var callback: ActivityCallback? = null
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "1"
    private val CHANNED_NAME = "name"

    override fun onCreate() {
        super.onCreate()
        Timber.tag(tag).d("Service started")

        resolveDaggerDependency()
        startForeground(NOTIFICATION_ID, createNotificationUploading())

        presenter.onAttach(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        presenter.onDetach()
        detachCallback()

        Timber.tag(tag).d("Service destroyed")
    }

    fun attachCallback(_callback: ActivityCallback) {
        callback = _callback
    }

    fun detachCallback() {
        callback = null
    }

    fun startPhotosUploading(userId: String, location: LonLat) {
        presenter.uploadPhotos(userId, location)
    }

    override fun onUploadingEvent(event: PhotoUploadingEvent) {
        callback?.onUploadingEvent(event)
    }

    override fun stopService() {
        Timber.e("Stopping service")

        updateUploadingNotificationShowSuccess()
        stopSelf()
    }

    //notifications
    private fun updateUploadingNotificationShowUploading() {
        val newNotification = createNotificationUploading()
        getNotificationManager().notify(NOTIFICATION_ID, newNotification)
    }

    private fun updateUploadingNotificationShowSuccess() {
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
                .setContentText("Photo has been uploaded!")
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(getNotificationIntent())
                .setAutoCancel(true)
                .build()
        } else {
            return NotificationCompat.Builder(this)
                .setContentTitle("Done")
                .setContentText("Photo has been uploaded!")
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
                .setAutoCancel(false)
                .setOngoing(true)
                .build()
        } else {
            return NotificationCompat.Builder(this)
                .setContentTitle("Please wait")
                .setContentText("Uploading photo...")
                .setSmallIcon(android.R.drawable.stat_sys_upload)
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
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE

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
        DaggerUploadPhotoServiceComponent.builder()
            .uploadPhotoServiceModule(UploadPhotoServiceModule(this))
            .uploadPhotoServicePresenterModule(UploadPhotoServicePresenterModule())
            .gsonModule(GsonModule())
            .databaseModule(DatabaseModule(Constants.DATABASE_NAME))
            .networkModule(NetworkModule(Constants.BASE_URL))
            .apiClientModule(ApiClientModule())
            .build()
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