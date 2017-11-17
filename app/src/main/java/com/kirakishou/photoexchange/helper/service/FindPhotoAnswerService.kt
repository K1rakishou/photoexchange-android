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
import com.kirakishou.photoexchange.helper.database.repository.PhotoAnswerRepository
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mwvm.model.other.ServerErrorCode
import com.kirakishou.photoexchange.mwvm.model.dto.PhotoAnswerReturnValue
import com.kirakishou.photoexchange.mwvm.model.event.PhotoReceivedEvent
import com.kirakishou.photoexchange.mwvm.viewmodel.FindPhotoAnswerServiceViewModel
import com.kirakishou.photoexchange.ui.activity.AllPhotosViewActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import javax.inject.Inject

class FindPhotoAnswerService : JobService() {

    @Inject
    lateinit var apiClient: ApiClient

    @Inject
    lateinit var schedulers: SchedulerProvider

    @Inject
    lateinit var photoAnswerRepo: PhotoAnswerRepository

    @Inject
    lateinit var eventBus: EventBus

    private lateinit var viewModel: FindPhotoAnswerServiceViewModel

    private val compositeDisposable = CompositeDisposable()
    private val NOTIFICATION_ID = 2

    override fun onCreate() {
        super.onCreate()
        Timber.d("FindPhotoAnswerService start")

        resolveDaggerDependency()
        viewModel = FindPhotoAnswerServiceViewModel(photoAnswerRepo, apiClient, schedulers)
    }

    override fun onDestroy() {
        Timber.d("FindPhotoAnswerService destroy")
        cleanUp()

        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("FindPhotoAnswerService onStartCommand")
        return START_NOT_STICKY
    }

    override fun onStartJob(params: JobParameters): Boolean {
        Timber.d("FindPhotoAnswerService onStartJob")

        when {
            params.jobId == IMMEDIATE_JOB_ID -> Timber.d("IMMEDIATE_JOB_ID")
            params.jobId == PERIODIC_JOB_ID -> Timber.d("PERIODIC_JOB_ID")
            else -> throw IllegalStateException("Unknown jobId ${params.jobId}")
        }

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

    private fun handleCommand(extras: PersistableBundle) {
        val userId = extras.getString("user_id")
        checkNotNull(userId)

        viewModel.inputs.findPhotoAnswer(userId)
    }

    override fun onStopJob(params: JobParameters): Boolean {
        Timber.d("FindPhotoAnswerService onStopJob")
        return true
    }

    private fun initRx(params: JobParameters) {
        compositeDisposable += viewModel.outputs.onPhotoAnswerFoundObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onPhotoAnswerFound(params, it) })

        compositeDisposable += viewModel.outputs.userHasNoUploadedPhotosObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ userHasNoUploadedPhotosObservable(params) })

        compositeDisposable += viewModel.outputs.noPhotosToSendBackObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ noPhotosToSendBackObservable(params) })

        compositeDisposable += viewModel.outputs.couldNotMarkPhotoAsReceivedObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ couldNotMarkPhotoAsReceived(params) })

        compositeDisposable += viewModel.outputs.uploadMorePhotosObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ uploadMorePhotos(params) })

        compositeDisposable += viewModel.errors.onBadResponseObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onBadResponse(params, it) })

        compositeDisposable += viewModel.errors.onUnknownErrorObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onUnknownError(params, it) })
    }

    fun uploadMorePhotos(params: JobParameters) {
        Timber.d("Upload more photos")

        eventBus.post(PhotoReceivedEvent.uploadMorePhotos())
        finish(params, false)
    }

    private fun couldNotMarkPhotoAsReceived(params: JobParameters) {
        Timber.d("Could not mark a photo as received")

        eventBus.post(PhotoReceivedEvent.fail())
        finish(params, true)
    }

    private fun noPhotosToSendBackObservable(params: JobParameters) {
        Timber.d("No photos to send back")

        eventBus.post(PhotoReceivedEvent.noPhotos())
        finish(params, false)
    }

    private fun userHasNoUploadedPhotosObservable(params: JobParameters) {
        Timber.d("User has no uploaded photos")

        eventBus.post(PhotoReceivedEvent.userNotUploadedPhotosYet())
        finish(params, false)
    }

    private fun onPhotoAnswerFound(params: JobParameters, returnValue: PhotoAnswerReturnValue) {
        if (!returnValue.allFound) {
            Timber.d("FindPhotoAnswerService: allFound = ${!returnValue.allFound} Reschedule job")
            Timber.e(returnValue.photoAnswer.toString())

            finish(params, true)
            return
        }

        Timber.d("FindPhotoAnswerService: allFound = ${!returnValue.allFound} we are done")
        eventBus.post(PhotoReceivedEvent.successAllReceived(returnValue.photoAnswer))
        finish(params, false)
    }

    private fun onBadResponse(params: JobParameters, errorCode: ServerErrorCode) {
        Timber.e("BadResponse: errorCode: $errorCode")

        //TODO: notify activity
        finish(params, true)
    }

    private fun onUnknownError(params: JobParameters, error: Throwable) {
        Timber.e("onUnknownError")
        Timber.e(error)

        //TODO: notify activity
        finish(params, false)
    }

    private fun finish(params: JobParameters, reschedule: Boolean) {
        Timber.d("finish, reschedule: $reschedule")
        jobFinished(params, reschedule)
    }

    private fun cleanUp() {
        Timber.e("FindPhotoAnswerService cleanUp()")

        compositeDisposable.clear()
        viewModel.cleanUp()
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
        private val IMMEDIATE_JOB_ID = 1
        private val PERIODIC_JOB_ID = 2

        fun scheduleImmediateJob(userId: String, context: Context) {
            val extras = PersistableBundle()
            extras.putString("user_id", userId)

            val jobInfo = JobInfo.Builder(IMMEDIATE_JOB_ID, ComponentName(context, FindPhotoAnswerService::class.java))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setRequiresDeviceIdle(false)
                    .setRequiresCharging(false)
                    .setMinimumLatency(0)
                    .setOverrideDeadline(0)
                    .setExtras(extras)
                    .setBackoffCriteria(5_000, JobInfo.BACKOFF_POLICY_LINEAR)
                    .build()

            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.cancelAll()
            jobScheduler.schedule(jobInfo)
        }

        fun scheduleJobPeriodicJob(userId: String, context: Context) {
            val extras = PersistableBundle()
            extras.putString("user_id", userId)

            val jobInfo = JobInfo.Builder(PERIODIC_JOB_ID, ComponentName(context, FindPhotoAnswerService::class.java))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setRequiresDeviceIdle(false)
                    .setRequiresCharging(false)
                    //TODO
                    .setMinimumLatency(1_000)
                    .setOverrideDeadline(6_000)
                    .setExtras(extras)
                    .setBackoffCriteria(5_000, JobInfo.BACKOFF_POLICY_LINEAR)
                    .build()

            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.cancelAll()
            jobScheduler.schedule(jobInfo)
        }
    }
}
