package com.kirakishou.photoexchange.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject
import android.app.NotificationManager
import android.app.NotificationChannel
import android.os.Build
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.activity.TakePhotoActivity


class PushNotificationReceiverService : FirebaseMessagingService() {
  private val TAG = "PushNotificationReceiverService"
  private val NOTIFICATION_ID = 3
  private val CHANNEL_ID by lazy { getString(R.string.default_notification_channel_id) }
  private val CHANNED_NAME = "name"

  @Inject
  lateinit var settingsRepository: SettingsRepository

  override fun onCreate() {
    super.onCreate()

    (application as PhotoExchangeApplication).applicationComponent
      .inject(this)
  }

  override fun onMessageReceived(remoteMessage: RemoteMessage?) {
    Timber.tag(TAG).d("onMessageReceived called")

    if (remoteMessage == null) {
      Timber.tag(TAG).w("Null remoteMessage has been received!")
      return
    }

    val photoExchanged = try {
      remoteMessage.data[photoExchangedFlag]?.toBoolean()
    } catch (error: Throwable) {
      null
    }

    if (photoExchanged != null && photoExchanged) {
      Timber.tag(TAG).d("Some photo has been exchanged")

      showNotification()
    }
  }

  override fun onNewToken(token: String?) {
    Timber.tag(TAG).d("onNewToken called")

    if (token == null) {
      Timber.tag(TAG).w("Null token has been received!")
      return
    }

    runBlocking {
      try {
        if (!settingsRepository.saveNewFirebaseToken(token)) {
          throw RuntimeException("Could not update new firebase token")
        }

        Timber.tag(TAG).d("Successfully updated firebase token")
      } catch (error: Throwable) {
        Timber.tag(TAG).e(error, "Could not update new firebase token")
      }
    }
  }

  private fun showNotification() {
    val backIntent = Intent(this, TakePhotoActivity::class.java).apply {
      addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }

    val intent = Intent(this, PhotosActivity::class.java).apply {
      putExtra(PhotosActivity.extraNewPhotoNotificationReceived, true)
    }

    val pendingIntent = PendingIntent.getActivities(
      this,
      0,
      arrayOf(backIntent, intent),
      PendingIntent.FLAG_ONE_SHOT
    )

    val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.stat_sys_download_done)
      .setContentTitle("You got a new photo from someone")
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)

    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        CHANNED_NAME,
        NotificationManager.IMPORTANCE_DEFAULT
      )

      channel.enableLights(true)
      channel.vibrationPattern = vibrationPattern

      notificationManager.createNotificationChannel(channel)
    }

    notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
  }

  companion object {
    private val vibrationPattern = LongArray(4).apply {
      this[0] = 0L
      this[1] = 300L
      this[2] = 200L
      this[3] = 300L
    }

    const val photoExchangedFlag = "photo_exchanged"
  }
}