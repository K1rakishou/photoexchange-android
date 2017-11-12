package com.kirakishou.photoexchange.helper.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import android.support.v4.app.NotificationCompat
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.component.DaggerFindPhotoAnswerServiceComponent
import com.kirakishou.photoexchange.di.component.DaggerUploadPhotoServiceComponent
import com.kirakishou.photoexchange.di.module.*
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvvm.model.PhotoAnswer
import com.kirakishou.photoexchange.mvvm.model.ServerErrorCode
import com.kirakishou.photoexchange.mvvm.model.ServiceCommand
import com.kirakishou.photoexchange.ui.activity.AllPhotosViewActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import timber.log.Timber
import javax.inject.Inject

class FindPhotoAnswerService : JobService() {

    @Inject
    lateinit var apiClient: ApiClient

    @Inject
    lateinit var schedulers: SchedulerProvider

    private lateinit var presenter: FindPhotoAnswerServicePresenter

    private val compositeDisposable = CompositeDisposable()
    private val NOTIFICATION_ID = 2

    override fun onCreate() {
        super.onCreate()
        Timber.e("FindPhotoAnswerService start")

        resolveDaggerDependency()
        presenter = FindPhotoAnswerServicePresenter(apiClient, schedulers)
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        presenter.detach()

        Timber.e("FindPhotoAnswerService destroy")
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onStartJob(params: JobParameters): Boolean {
        initRx(params)

        try {
            handleCommand(params.extras)
        } catch (error: Throwable) {
            onUnknownError(params, false, error)
            return false
        }

        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        compositeDisposable.clear()
        presenter.detach()

        return true
    }

    private fun initRx(params: JobParameters) {
        compositeDisposable += presenter.outputs.onPhotoAnswerFoundObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onPhotoAnswerFound)

        compositeDisposable += presenter.errors.onBadResponseObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onBadResponse(params, true, it) })

        compositeDisposable += presenter.errors.onUnknownErrorObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onUnknownError(params, true, it) })
    }

    private fun handleCommand(extras: PersistableBundle) {
        val commandRaw = extras.getInt("command", -1)
        check(commandRaw != -1)

        val serviceCommand = ServiceCommand.from(commandRaw)
        when (serviceCommand) {
            ServiceCommand.FIND_PHOTO -> {
                val userId = extras.getString("user_id")
                checkNotNull(userId)

                presenter.inputs.findPhotoAnswer(userId)
            }

            else -> throw IllegalArgumentException("Unknown serviceCommand: $serviceCommand")
        }
    }

    private fun onPhotoAnswerFound(photoAnswer: PhotoAnswer) {
        Timber.e(photoAnswer.toString())
    }

    private fun onBadResponse(params: JobParameters, reschedule: Boolean, errorCode: ServerErrorCode) {
        Timber.e("BadResponse: errorCode: $errorCode")

        //TODO: should notify activity

        finish(params, reschedule)
    }

    private fun onUnknownError(params: JobParameters, reschedule: Boolean, error: Throwable) {
        Timber.e(error)

        //TODO: should notify activity

        finish(params, reschedule)
    }

    private fun finish(params: JobParameters, reschedule: Boolean) {
        jobFinished(params, reschedule)
    }

    private fun startAsForeground() {
        val notification = NotificationCompat.Builder(this)
                .setContentTitle("Please wait")
                .setContentText("Looking for a photo...")
                .setSmallIcon(android.R.drawable.ic_menu_search)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(getNotificationIntent())
                .setAutoCancel(false)
                .setOngoing(true)
                .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun updateFindNotificationShowSuccess() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val newNotification = NotificationCompat.Builder(this)
                .setContentTitle("Done")
                .setContentText("Photo has been found!")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(getNotificationIntent())
                .setAutoCancel(false)
                .build()

        notificationManager.notify(NOTIFICATION_ID, newNotification)
    }

    private fun getNotificationIntent(): PendingIntent {
        val notificationIntent = Intent(this, AllPhotosViewActivity::class.java)
        notificationIntent.action = Intent.ACTION_MAIN
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun resolveDaggerDependency() {
        DaggerFindPhotoAnswerServiceComponent
                .builder()
                .findPhotoAnswerServiceModule(FindPhotoAnswerServiceModule(this))
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

    companion object {
        val TAG = "FindPhotoAnswerServiceTag"
    }
}
