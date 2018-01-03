package com.kirakishou.photoexchange.helper.service

import android.app.*
import android.content.Intent
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import timber.log.Timber
import javax.inject.Inject
import android.content.Context
import android.support.v4.app.NotificationCompat
import com.crashlytics.android.Crashlytics
import com.kirakishou.photoexchange.di.component.DaggerUploadPhotoServiceComponent
import com.kirakishou.photoexchange.di.module.*
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mwvm.model.event.PhotoUploadedEvent
import com.kirakishou.photoexchange.mwvm.model.other.*
import com.kirakishou.photoexchange.mwvm.viewmodel.UploadPhotoServiceViewModel
import io.fabric.sdk.android.Fabric
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import org.greenrobot.eventbus.EventBus
import android.app.NotificationManager
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.os.Build
import android.support.annotation.RequiresApi
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.mwvm.model.state.PhotoUploadingState
import com.kirakishou.photoexchange.ui.activity.AllPhotosViewActivity


class UploadPhotoService : JobService() {

    @Inject
    lateinit var apiClient: ApiClient

    @Inject
    lateinit var schedulers: SchedulerProvider

    @Inject
    lateinit var eventBus: EventBus

    @Inject
    lateinit var takenPhotosRepo: TakenPhotosRepository

    private var notificationManager: NotificationManager? = null

    private val tag = "[${this::class.java.simpleName}]: "
    private var isRxInited = false
    private val compositeDisposable = CompositeDisposable()
    private lateinit var viewModel: UploadPhotoServiceViewModel

    override fun onCreate() {
        super.onCreate()

        Fabric.with(this, Crashlytics())
        resolveDaggerDependency()
        viewModel = UploadPhotoServiceViewModel(apiClient, schedulers, takenPhotosRepo)
    }

    override fun onDestroy() {
        cleanUp()

        super.onDestroy()
    }

    private fun cleanUp() {
        compositeDisposable.clear()
        viewModel.cleanUp()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onStartJob(params: JobParameters): Boolean {
        Timber.tag(tag).d("onStartJob() onStartJob")
        initRx(params)

        try {
            handleCommand(params)
        } catch (error: Throwable) {
            onUnknownError(params, error)
            return false
        }

        //startAsForeground()
        return true
    }

    private fun handleCommand(params: JobParameters) {
        viewModel.inputs.uploadPhotos()
    }

    override fun onStopJob(params: JobParameters): Boolean {
        Timber.tag(tag).d("onStopJob() onStopJob")
        return true
    }

    private fun initRx(params: JobParameters) {
        if (isRxInited) {
            return
        }

        isRxInited = true

        compositeDisposable += viewModel.outputs.onPhotoUploadStateObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ status -> onPhotoUploadState(params, status) })

        compositeDisposable += viewModel.errors.onBadResponseObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onBadResponse(params, it) }, { onUnknownError(params, it) })

        compositeDisposable += viewModel.errors.onUnknownErrorObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onUnknownError(params, it) }, { onUnknownError(params, it) })
    }

    private fun onPhotoUploadState(params: JobParameters, state: PhotoUploadingState) {
        when (state) {
            is PhotoUploadingState.NoPhotoToUpload -> {
                Timber.tag(tag).d("onPhotoUploadState() PhotoUploadingState.NoPhotoToUpload")
                finish(params, false)
            }

            is PhotoUploadingState.StartPhotoUploading -> {
                Timber.tag(tag).d("onPhotoUploadState() PhotoUploadingState.StartPhotoUploading")
                sendEvent(PhotoUploadedEvent.startUploading())
                updateUploadingNotificationShowUploading()
            }

            is PhotoUploadingState.PhotoUploaded -> {
                Timber.tag(tag).d("onPhotoUploadState() PhotoUploadingState.PhotoUploaded photoName: ${state.photo!!.photoName}")
                sendEvent(PhotoUploadedEvent.photoUploaded(state.photo))
            }

            is PhotoUploadingState.FailedToUploadPhoto -> {
                Timber.tag(tag).d("onPhotoUploadState() PhotoUploadingState.FailedToUploadPhoto")
                sendEvent(PhotoUploadedEvent.fail(state.photo))
            }

            is PhotoUploadingState.AllPhotosUploaded -> {
                Timber.tag(tag).d("onPhotoUploadState() PhotoUploadingState.AllPhotosUploaded")

                sendEvent(PhotoUploadedEvent.done())
                updateUploadingNotificationShowSuccess()
                finish(params, false)
            }

            is PhotoUploadingState.UnknownErrorWhileUploading -> {
                Timber.tag(tag).d("onPhotoUploadState() PhotoUploadingState.UnknownErrorWhileUploading")
            }
        }
    }

    private fun onBadResponse(params: JobParameters, errorCode: ServerErrorCode) {
        Timber.tag(tag).d("onBadResponse() BadResponse: errorCode: $errorCode")

        updateUploadingNotificationShowError()
        finish(params, false)
    }

    private fun onUnknownError(params: JobParameters, error: Throwable) {
        Timber.tag(tag).d("onUnknownError() Unknown error: $error")

        updateUploadingNotificationShowError()
        finish(params, false)
    }

    private fun sendEvent(event: PhotoUploadedEvent) {
        eventBus.post(event)
    }

    private fun finish(params: JobParameters, reschedule: Boolean) {
        jobFinished(params, reschedule)
    }

    private fun updateUploadingNotificationShowUploading() {
        val newNotification = createNotificationUploading()
        getNotificationManager().notify(Constants.NOTIFICATION_ID, newNotification)
    }

    private fun updateUploadingNotificationShowSuccess() {
        val newNotification = createNotificationSuccess()
        getNotificationManager().notify(Constants.NOTIFICATION_ID, newNotification)
    }

    private fun updateUploadingNotificationShowError() {
        val newNotification = createNotificationError()
        getNotificationManager().notify(Constants.NOTIFICATION_ID, newNotification)
    }

    private fun createNotificationError(): Notification {
        if (AndroidUtils.isOreoOrHigher()) {
            createNotificationChannelIfNotExists()

            return NotificationCompat.Builder(this, Constants.CHANNEL_ID)
                    .setContentTitle("Error")
                    .setContentText("Could not upload photo")
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setWhen(System.currentTimeMillis())
                    .setContentIntent(getNotificationIntent())
                    .setAutoCancel(true)
                    .build()
        } else {
            return NotificationCompat.Builder(this, Constants.CHANNEL_ID)
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

            return NotificationCompat.Builder(this, Constants.CHANNEL_ID)
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

            return NotificationCompat.Builder(this, Constants.CHANNEL_ID)
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
        if (getNotificationManager().getNotificationChannel(Constants.CHANNEL_ID) == null) {
            val notificationChannel = NotificationChannel(Constants.CHANNEL_ID, Constants.CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW)

            notificationChannel.enableLights(false)
            notificationChannel.enableVibration(false)
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE

            getNotificationManager().createNotificationChannel(notificationChannel)
        }
    }

    private fun getNotificationIntent(): PendingIntent {
        val notificationIntent = Intent(this, AllPhotosViewActivity::class.java)
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

    companion object {
        private val JOB_ID = 2

        //Add slight delay so we have time to cancel previous job if the schedule
        //function was called twice over minimumDelay period of time
        fun scheduleJob(context: Context, minimumDelay: Long = 5_000) {
            val jobInfo = JobInfo.Builder(JOB_ID, ComponentName(context, UploadPhotoService::class.java))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setRequiresDeviceIdle(false)
                    .setRequiresCharging(false)
                    .setMinimumLatency(minimumDelay)
                    .setOverrideDeadline(0)
                    .setBackoffCriteria(1_000, JobInfo.BACKOFF_POLICY_LINEAR)
                    .build()

            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.cancel(JOB_ID)
            val result = jobScheduler.schedule(jobInfo)

            check(result == JobScheduler.RESULT_SUCCESS)
        }

        fun scheduleJobWhenWiFiAvailable(context: Context, minimumDelay: Long = 5_000) {
            val jobInfo = JobInfo.Builder(JOB_ID, ComponentName(context, UploadPhotoService::class.java))
                    //TODO:
                    //HACK: Google's emulators do not support changing internet type to Wi-Fi
                    //therefore we need this hack to be able to test the app on google's emulators
                    //Change this on release build!!!
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setRequiresDeviceIdle(false)
                    .setRequiresCharging(false)
                    .setMinimumLatency(minimumDelay)
                    .setOverrideDeadline(0)
                    .setBackoffCriteria(5_000, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                    .build()

            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.cancel(JOB_ID)
            val result = jobScheduler.schedule(jobInfo)

            check(result == JobScheduler.RESULT_SUCCESS)
        }

        fun isAlreadyRunning(context: Context): Boolean {
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            return scheduler.allPendingJobs.any { it.id == JOB_ID }
        }
    }
}
