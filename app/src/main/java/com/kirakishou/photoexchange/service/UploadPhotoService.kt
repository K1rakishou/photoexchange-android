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
import com.kirakishou.photoexchange.di.module.*
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.location.LocationService
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.callback.PhotoUploadingCallback
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * Created by kirakishou on 3/11/2018.
 */
class UploadPhotoService : Service() {

    @Inject
    lateinit var presenter: UploadPhotoServicePresenter

    @Inject
    lateinit var locationService: LocationService

    private val TAG = "UploadPhotoService"

    private var notificationManager: NotificationManager? = null
    private val binder = UploadPhotosBinder()
    private val compositeDisposable = CompositeDisposable()

    private var callback = WeakReference<PhotoUploadingCallback>(null)
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "1"
    private val CHANNED_NAME = "name"

    override fun onCreate() {
        super.onCreate()
        Timber.tag(TAG).d("UploadPhotoService started")

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
        compositeDisposable.clear()

        Timber.tag(TAG).d("UploadPhotoService destroyed")
    }

    fun attachCallback(_callback: WeakReference<PhotoUploadingCallback>) {
        Timber.tag(TAG).d("attachCallback")
        callback = _callback
    }

    fun detachCallback() {
        Timber.tag(TAG).d("detachCallback")
        callback = WeakReference<PhotoUploadingCallback>(null)
    }

    fun startPhotosUploading() {
        requireNotNull(callback.get())

        compositeDisposable += locationService.getCurrentLocation()
            .subscribe({ location ->
                presenter.uploadPhotos(location)
            }, { error ->
                Timber.tag(TAG).e(error)
            })
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

    private fun createInitialNotification(): Notification {
        if (AndroidUtils.isOreoOrHigher()) {
            createNotificationChannelIfNotExists()

            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Please wait")
                .setContentText("Uploading photo...")
                .setWhen(System.currentTimeMillis())
                .setContentIntent(getNotificationIntent())
                .setAutoCancel(false)
                .setOngoing(true)
                .build()
        } else {
            return NotificationCompat.Builder(this)
                .setContentTitle("Please wait")
                .setContentText("Uploading photo...")
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

    companion object {
        //TODO: probably delete
        fun isRunning(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
            for (service in manager!!.getRunningServices(Integer.MAX_VALUE)) {
                if (UploadPhotoService::class.java.name == service.service.className) {
                    return true
                }
            }
            return false
        }
    }
}