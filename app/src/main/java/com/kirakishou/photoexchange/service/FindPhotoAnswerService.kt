package com.kirakishou.photoexchange.service

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PersistableBundle

class FindPhotoAnswerService : JobService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onStartJob(params: JobParameters): Boolean {
        val userId = params.extras.getString("user_id")

        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return true
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
                .setMinimumLatency(5_000)
                .setOverrideDeadline(30_000)
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