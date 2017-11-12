package com.kirakishou.photoexchange.helper.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import android.support.v4.app.NotificationCompat
import com.kirakishou.photoexchange.mvvm.model.ServiceCommand
import com.kirakishou.photoexchange.ui.activity.AllPhotosViewActivity
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber

class FindPhotoAnswerService : JobService() {
    private lateinit var presenter: FindPhotoAnswerServicePresenter

    private val compositeDisposable = CompositeDisposable()
    private val NOTIFICATION_ID = 2

    override fun onCreate() {
        super.onCreate()
        Timber.e("FindPhotoAnswerService start")
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        presenter.detach()

        Timber.e("FindPhotoAnswerService destroy")
        super.onDestroy()
    }

    override fun onStartJob(params: JobParameters): Boolean {
        handleCommand(params.extras)
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        compositeDisposable.clear()
        presenter.detach()

        return true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        /*if (intent != null) {
            handleCommand(intent)
            startAsForeground()
        }*/

        return START_NOT_STICKY
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

            else -> onUnknownError(IllegalArgumentException("Unknown serviceCommand: $serviceCommand"))
        }
    }

    private fun onUnknownError(error: Throwable) {
        Timber.e("Unknown error: $error")

        stopService()
    }

    private fun stopService() {
        Timber.d("Stopping service")

        stopForeground(false)
        stopSelf()
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
}
