package com.kirakishou.photoexchange.helper.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.component.DaggerMainActivityComponent
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvvm.model.LonLat
import com.kirakishou.photoexchange.mvvm.model.ServiceCommand
import timber.log.Timber
import javax.inject.Inject
import com.kirakishou.photoexchange.ui.activity.MainActivity
import android.app.PendingIntent
import com.kirakishou.photoexchange.di.component.DaggerServiceComponent
import com.kirakishou.photoexchange.di.module.*
import com.kirakishou.photoexchange.mvvm.model.ServerErrorCode
import com.kirakishou.photoexchange.mvvm.model.net.response.StatusResponse
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign


class SendPhotoService : Service() {

    @Inject
    lateinit var apiClient: ApiClient

    @Inject
    lateinit var schedulers: SchedulerProvider

    private val NOTIFICATION_ID = 1

    private val compositeDisposable = CompositeDisposable()
    private lateinit var presenter: SendPhotoServicePresenter

    override fun onCreate() {
        Timber.e("SendPhotoService start")
        super.onCreate()

        resolveDaggerDependency()

        presenter = SendPhotoServicePresenter(apiClient, schedulers)
        initRx()
    }

    override fun onDestroy() {
        Timber.e("SendPhotoService destroy")
        compositeDisposable.clear()
        presenter.detach()

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

    private fun onSendPhotoResponseObservable(errorCode: ServerErrorCode) {
        Timber.d("onSendPhotoResponseObservable() errorCode: $errorCode")

        stopService()
    }

    private fun onBadResponse(errorCode: ServerErrorCode) {
        Timber.e("BadResponse: errorCode: $errorCode")

        stopService()
    }

    private fun onUnknownError(error: Throwable) {
        Timber.e("Unknown error: $error")

        stopService()
    }

    private fun stopService() {
        Timber.d("Stopping service")

        stopForeground(true)
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            handleCommand(intent)
            showNotification()
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

    private fun showNotification() {
        val notificationIntent = Intent(applicationContext, MainActivity::class.java)
        notificationIntent.action = Intent.ACTION_MAIN
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val contentIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = Notification.Builder(applicationContext)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Text")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()

        startForeground(NOTIFICATION_ID, notification)
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
                .build()
                .inject(this)
    }
}
