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
import com.kirakishou.photoexchange.helper.extension.seconds
import com.kirakishou.photoexchange.helper.location.MyLocationManager
import com.kirakishou.photoexchange.helper.location.RxLocationManager
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.model.PhotoUploadEvent
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.callback.PhotoUploadingCallback
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Created by kirakishou on 3/11/2018.
 */
class UploadPhotoService : Service(), UploadPhotoServiceCallbacks {

    @Inject
    lateinit var presenter: UploadPhotoServicePresenter

    private val tag = "UploadPhotoService"

    private val locationManager by lazy { MyLocationManager(applicationContext) }
    private var notificationManager: NotificationManager? = null
    private val binder = UploadPhotosBinder()
    private val compositeDisposable = CompositeDisposable()

    private val GPS_DELAY_MS = 1.seconds()
    private val GPS_LOCATION_OBTAINING_MAX_TIMEOUT_MS = 15.seconds()
    private var callback = WeakReference<PhotoUploadingCallback>(null)
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "1"
    private val CHANNED_NAME = "name"

    override fun onCreate() {
        super.onCreate()
        Timber.tag(tag).d("UploadPhotoService started")

        resolveDaggerDependency()
        startForeground(NOTIFICATION_ID, createNotificationUploading())
    }

    override fun onDestroy() {
        super.onDestroy()

        presenter.onDetach()
        detachCallback()
        compositeDisposable.clear()

        Timber.tag(tag).d("UploadPhotoService destroyed")
    }

    fun attachCallback(_callback: WeakReference<PhotoUploadingCallback>) {
        Timber.tag(tag).d("attachCallback")
        callback = _callback
    }

    fun detachCallback() {
        Timber.tag(tag).d("detachCallback")
        callback = WeakReference<PhotoUploadingCallback>(null)
    }

    fun startPhotosUploading() {
        requireNotNull(callback.get())

        updateUploadingNotificationShowUploading()
        presenter.uploadPhotos()
    }

    override fun onUploadingEvent(event: PhotoUploadEvent) {
        callback.get()?.onUploadPhotosEvent(event)

        if (event is PhotoUploadEvent.OnEnd) {
            if (event.allUploaded) {
                updateUploadingNotificationShowSuccess("All photos has been successfully uploaded")
            } else {
                updateUploadingNotificationShowError("Could not upload one or more photos")
            }
        }
    }

    override fun onError(error: Throwable) {
        Timber.e(error)
        updateUploadingNotificationShowError(error.message ?: "Could not upload photos. Unknown error.")
    }

    override fun stopService() {
        Timber.tag(tag).d("Stopping service")

        stopForeground(true)
        stopSelf()
    }

    override fun getCurrentLocation(): Single<LonLat> {
        return Single.fromCallable { locationManager.isGpsEnabled() }
            .flatMap { isGpsEnabled ->
                if (!isGpsEnabled) {
                    return@flatMap Single.just(LonLat.empty())
                }

                return@flatMap RxLocationManager.start(locationManager)
                    .observeOn(Schedulers.io())
                    .single(LonLat.empty())
                    .timeout(GPS_LOCATION_OBTAINING_MAX_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .onErrorReturnItem(LonLat.empty())
            }
            .delay(GPS_DELAY_MS, TimeUnit.MILLISECONDS)
    }

    //notifications
    private fun updateUploadingNotificationShowUploading() {
        val newNotification = createNotificationUploading()
        getNotificationManager().notify(NOTIFICATION_ID, newNotification)
    }

    private fun updateUploadingNotificationShowSuccess(message: String) {
        val newNotification = createNotificationSuccess(message)
        getNotificationManager().notify(NOTIFICATION_ID, newNotification)
    }

    private fun updateUploadingNotificationShowError(message: String) {
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
            .plus(UploadPhotoServiceModule(this))
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