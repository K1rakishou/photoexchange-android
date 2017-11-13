package com.kirakishou.photoexchange.helper.service

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mwvm.model.other.LonLat
import com.kirakishou.photoexchange.mwvm.model.other.ServiceCommand
import timber.log.Timber
import javax.inject.Inject
import com.kirakishou.photoexchange.ui.activity.TakePhotoActivity
import android.app.PendingIntent
import android.content.Context
import android.support.v4.app.NotificationCompat
import com.kirakishou.photoexchange.di.component.DaggerUploadPhotoServiceComponent
import com.kirakishou.photoexchange.di.module.*
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.mwvm.model.other.ServerErrorCode
import com.kirakishou.photoexchange.mwvm.model.other.UploadedPhoto
import com.kirakishou.photoexchange.mwvm.model.event.PhotoUploadedEvent
import com.kirakishou.photoexchange.mwvm.viewmodel.UploadPhotoServiceViewModel
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
    private lateinit var viewModel: UploadPhotoServiceViewModel

    override fun onCreate() {
        super.onCreate()
        Timber.d("UploadPhotoService start")

        resolveDaggerDependency()

        viewModel = UploadPhotoServiceViewModel(apiClient, schedulers, uploadedPhotosRepo)
        initRx()
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        viewModel.detach()

        Timber.d("UploadPhotoService destroy")
        super.onDestroy()
    }

    private fun initRx() {
        compositeDisposable += viewModel.outputs.onSendPhotoResponseObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onSendPhotoResponseObservable)

        compositeDisposable += viewModel.errors.onBadResponseObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onBadResponse)

        compositeDisposable += viewModel.errors.onUnknownErrorObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onUnknownError)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            handleCommand(intent)
            startAsForeground()
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

                viewModel.inputs.uploadPhoto(photoFilePath, location, userId)
            }

            else -> onUnknownError(IllegalArgumentException("Unknown serviceCommand: $serviceCommand"))
        }
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

    private fun startAsForeground() {
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
        DaggerUploadPhotoServiceComponent
                .builder()
                .uploadPhotoServiceModule(UploadPhotoServiceModule(this))
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
