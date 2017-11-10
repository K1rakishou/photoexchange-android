package com.kirakishou.photoexchange.helper.service

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvvm.model.LonLat
import com.kirakishou.photoexchange.mvvm.model.ServiceCommand
import timber.log.Timber
import javax.inject.Inject
import com.kirakishou.photoexchange.ui.activity.TakePhotoActivity
import android.app.PendingIntent
import android.content.Context
import android.support.v4.app.NotificationCompat
import com.kirakishou.photoexchange.di.component.DaggerServiceComponent
import com.kirakishou.photoexchange.di.module.*
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.mvvm.model.ServerErrorCode
import com.kirakishou.photoexchange.mvvm.model.UploadedPhoto
import com.kirakishou.photoexchange.mvvm.model.event.PhotoUploadedEvent
import com.kirakishou.photoexchange.mvvm.model.event.SendPhotoEventStatus
import com.kirakishou.photoexchange.ui.activity.AllPhotosViewActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import org.greenrobot.eventbus.EventBus


class UploadPhotoService : Service() {

    @Inject
    lateinit var apiClient: ApiClient

    @Inject
    lateinit var schedulers: SchedulerProvider

    @Inject
    lateinit var eventBus: EventBus

    @Inject
    lateinit var uploadedPhotosRepo: UploadedPhotosRepository

    private val NOTIFICATION_ID = 1

    private val compositeDisposable = CompositeDisposable()
    private lateinit var presenter: UploadPhotoServicePresenter

    override fun onCreate() {
        super.onCreate()
        Timber.e("UploadPhotoService start")

        resolveDaggerDependency()

        presenter = UploadPhotoServicePresenter(apiClient, schedulers, uploadedPhotosRepo)
        initRx()
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        presenter.detach()

        Timber.e("UploadPhotoService destroy")
        super.onDestroy()
    }

    private fun initRx() {
        compositeDisposable += presenter.outputs.onSendPhotoResponseObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onSendPhotoResponseObservable)

        compositeDisposable += presenter.errors.onBadResponseObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onBadResponse)

        compositeDisposable += presenter.errors.onUnknownErrorObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onUnknownError)
    }

    private fun onSendPhotoResponseObservable(uploadedPhoto: UploadedPhoto) {
        Timber.d("onSendPhotoResponseObservable() photoName: ${uploadedPhoto.photoName}")

        eventBus.post(PhotoUploadedEvent.success(uploadedPhoto))

        updateUploadingNotificationShowSuccess()
        stopService()
    }

    private fun onBadResponse(errorCode: ServerErrorCode) {
        Timber.e("BadResponse: errorCode: $errorCode")

        eventBus.post(PhotoUploadedEvent.fail())

        updateUploadingNotificationShowError()
        stopService()
    }

    private fun onUnknownError(error: Throwable) {
        Timber.e("Unknown error: $error")

        eventBus.post(PhotoUploadedEvent.fail())

        updateUploadingNotificationShowError()
        stopService()
    }

    private fun stopService() {
        Timber.d("Stopping service")

        stopForeground(false)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            handleCommand(intent)
            showUploadingNotification()
        }

        return START_NOT_STICKY
    }

    private fun handleCommand(intent: Intent) {
        val commandRaw = intent.getIntExtra("command", -1)
        check(commandRaw != -1)

        val serviceCommand = ServiceCommand.from(commandRaw)
        when (serviceCommand) {
            ServiceCommand.SEND_PHOTO -> {
                val lon = intent.getDoubleExtra("lon", 0.0)
                val lat = intent.getDoubleExtra("lat", 0.0)
                val userId = intent.getStringExtra("user_id")
                val photoFilePath = intent.getStringExtra("photo_file_path")
                val location = LonLat(lon, lat)

                presenter.inputs.uploadPhoto(photoFilePath, location, userId)
            }
        }
    }

    private fun showUploadingNotification() {
        val notification = NotificationCompat.Builder(this)
                .setContentTitle("Please wait")
                .setContentText("Uploading photo...")
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(getNotificationIntent())
                .setAutoCancel(false)
                .setOngoing(true)
                .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun updateUploadingNotificationShowSuccess() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val newNotification = NotificationCompat.Builder(this)
                .setContentTitle("Done")
                .setContentText("Photo has been uploaded!")
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(getNotificationIntent())
                .setAutoCancel(false)
                .build()

        notificationManager.notify(NOTIFICATION_ID, newNotification)
    }

    private fun updateUploadingNotificationShowError() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val newNotification = NotificationCompat.Builder(this)
                .setContentTitle("Error")
                .setContentText("Could not upload photo")
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(getNotificationIntent())
                .setAutoCancel(false)
                .build()

        notificationManager.notify(NOTIFICATION_ID, newNotification)
    }

    private fun getNotificationIntent(): PendingIntent {
        val notificationIntent = Intent(this, TakePhotoActivity::class.java)
        notificationIntent.action = Intent.ACTION_MAIN
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onBind(intent: Intent): IBinder? = null

    private fun resolveDaggerDependency() {
        DaggerServiceComponent
                .builder()
                .serviceModule(ServiceModule(this))
                .networkModule(NetworkModule(PhotoExchangeApplication.baseUrl))
                .gsonModule(GsonModule())
                .apiClientModule(ApiClientModule())
                .schedulerProviderModule(SchedulerProviderModule())
                .eventBusModule(EventBusModule())
                .databaseModule(DatabaseModule(PhotoExchangeApplication.databaseName))
                .mapperModule(MapperModule())
                .build()
                .inject(this)
    }
}
