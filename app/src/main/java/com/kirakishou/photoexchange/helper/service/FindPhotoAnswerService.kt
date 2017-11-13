package com.kirakishou.photoexchange.helper.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import android.support.v4.app.NotificationCompat
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.component.DaggerFindPhotoAnswerServiceComponent
import com.kirakishou.photoexchange.di.module.*
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvvm.model.other.ServerErrorCode
import com.kirakishou.photoexchange.mvvm.model.other.ServiceCommand
import com.kirakishou.photoexchange.mvvm.model.dto.PhotoAnswerReturnValue
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
        Timber.d("FindPhotoAnswerService start")

        resolveDaggerDependency()
        presenter = FindPhotoAnswerServicePresenter(apiClient, schedulers)
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        presenter.detach()

        Timber.d("FindPhotoAnswerService destroy")
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("FindPhotoAnswerService onStartCommand")
        return START_NOT_STICKY
    }

    override fun onStartJob(params: JobParameters): Boolean {
        Timber.d("FindPhotoAnswerService onStartJob")

        val currentTime = TimeUtils.getTimeFast()
        val formattedTime = TimeUtils.formatDateAndTime(currentTime)
        Timber.e("Job started at: $formattedTime")

        initRx(params)

        try {
            handleCommand(params.extras)
        } catch (error: Throwable) {
            onUnknownError(params, error)
            return false
        }

        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        Timber.d("FindPhotoAnswerService onStopJob")

        compositeDisposable.clear()
        presenter.detach()

        return true
    }

    private fun initRx(params: JobParameters) {
        compositeDisposable += presenter.outputs.onPhotoAnswerFoundObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onPhotoAnswerFound(params, it) })

        compositeDisposable += presenter.errors.onBadResponseObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onBadResponse(params, it) })

        compositeDisposable += presenter.errors.onUnknownErrorObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onUnknownError(params, it) })
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

            else -> throw IllegalArgumentException("FindPhotoAnswerService Unknown serviceCommand: $serviceCommand")
        }
    }

    private fun onPhotoAnswerFound(params: JobParameters, returnValue: PhotoAnswerReturnValue) {
        if (!returnValue.allFound) {
            Timber.d("FindPhotoAnswerService Reschedule job")
        }

        Timber.e(returnValue.toString())

        finish(params, true)
    }

    private fun onBadResponse(params: JobParameters, errorCode: ServerErrorCode) {
        Timber.e("BadResponse: errorCode: $errorCode")

        //TODO: should notify activity

        finish(params, true)
    }

    private fun onUnknownError(params: JobParameters, error: Throwable) {
        Timber.e(error)

        //TODO: should notify activity

        finish(params, false)
    }

    private fun finish(params: JobParameters, reschedule: Boolean) {
        Timber.d("finish, reschedule: $reschedule")
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
        private val JOB_ID = 1

        fun scheduleImmediateJob(userId: String, context: Context) {
            val extras = PersistableBundle()
            extras.putInt("command", ServiceCommand.FIND_PHOTO.value)
            extras.putString("user_id", userId)

            val jobInfo = JobInfo.Builder(JOB_ID, ComponentName(context, FindPhotoAnswerService::class.java))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setRequiresDeviceIdle(false)
                    .setRequiresCharging(false)
                    .setMinimumLatency(0)
                    .setOverrideDeadline(0)
                    .setExtras(extras)
                    .setBackoffCriteria(5_000, JobInfo.BACKOFF_POLICY_LINEAR)
                    .build()

            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.cancel(JOB_ID)
            jobScheduler.schedule(jobInfo)
        }

        fun scheduleJobPeriodicJob(userId: String, context: Context) {
            val extras = PersistableBundle()
            extras.putInt("command", ServiceCommand.FIND_PHOTO.value)
            extras.putString("user_id", userId)

            val jobInfo = JobInfo.Builder(JOB_ID, ComponentName(context, FindPhotoAnswerService::class.java))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setRequiresDeviceIdle(false)
                    .setRequiresCharging(false)
                    .setMinimumLatency(10_000)
                    .setOverrideDeadline(60_000)
                    .setExtras(extras)
                    .setBackoffCriteria(5_000, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                    .build()

            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.cancel(JOB_ID)
            jobScheduler.schedule(jobInfo)
        }
    }
}
