package com.kirakishou.photoexchange.service

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.module.FindPhotoAnswerServiceModule
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class FindPhotoAnswerService : JobService() {

    @Inject
    lateinit var presenter: FindPhotoAnswerServicePresenter

    private val tag = "[${this::class.java.simpleName}] "
    private val compositeDisposable = CompositeDisposable()

    override fun onCreate() {
        super.onCreate()
        Timber.tag(tag).d("Service started")

        resolveDaggerDependency()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.tag(tag).d("Service destroyed")

        compositeDisposable.clear()
    }

    override fun onStartJob(params: JobParameters): Boolean {
        val userId = params.extras.getString("user_id")
        compositeDisposable += presenter.startFindPhotoAnswers(userId)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe()

        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return true
    }

    private fun resolveDaggerDependency() {
        (application as PhotoExchangeApplication).applicationComponent
            .plus(FindPhotoAnswerServiceModule(this))
            .inject(this)
    }

    companion object {
        private val JOB_ID = 1

        fun scheduleJob(userId: String, context: Context) {
            val extras = PersistableBundle()
            extras.putString("user_id", userId)

            val jobInfo = JobInfo.Builder(JOB_ID, ComponentName(context, FindPhotoAnswerService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .setRequiresDeviceIdle(false)
                .setRequiresCharging(false)
                .setMinimumLatency(1_000)
                .setOverrideDeadline(5_000)
                .setExtras(extras)
                .setBackoffCriteria(5_000, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                .build()

            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.schedule(jobInfo)
        }

        fun isAlreadyRunning(context: Context): Boolean {
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            return scheduler.allPendingJobs.any { it.id == JOB_ID }
        }
    }
}