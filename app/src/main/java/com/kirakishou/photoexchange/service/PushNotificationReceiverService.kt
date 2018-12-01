package com.kirakishou.photoexchange.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.interactors.StorePhotoFromPushNotificationUseCase
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.activity.TakePhotoActivity
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject


class PushNotificationReceiverService : FirebaseMessagingService() {
  private val TAG = "PushNotificationReceiverService"
  private val CHANNEL_ID by lazy { getString(R.string.default_notification_channel_id) }
  private val CHANNED_NAME = "name"

  @Inject
  lateinit var settingsRepository: SettingsRepository

  @Inject
  lateinit var storePhotoFromPushNotificationUseCase: StorePhotoFromPushNotificationUseCase

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

    val photoExchangedData = try {
      extractData(remoteMessage)
    } catch (error: Throwable) {
      Timber.tag(TAG).e(error)
      null
    }

    if (photoExchangedData != null) {
      Timber.tag(TAG).d("Got new photo from someone")

      runBlocking {
        if (!storePhotoInDatabase(photoExchangedData)) {
          Timber.tag(TAG).w("Could not store photoExchangedData")
          return@runBlocking
        }

        showNotification()
        sendPhotoReceivedBroadcast()
      }
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

  private suspend fun storePhotoInDatabase(photoExchangedData: PhotoExchangedData): Boolean {
    return storePhotoFromPushNotificationUseCase.storePhoto(photoExchangedData)
  }

  private fun extractData(remoteMessage: RemoteMessage): PhotoExchangedData? {
    val data = remoteMessage.data

    val uploadedPhotoName = data.get(uploadedPhotoNameField)
    if (uploadedPhotoName.isNullOrEmpty()) {
      return null
    }

    val receivedPhotoName = data.get(receivedPhotoNameField)
    if (receivedPhotoName.isNullOrEmpty()) {
      return null
    }

    val receiverLon = data.get(receiverLonField)?.toDoubleOrNull()
      ?: return null

    val receiverLat = data.get(receiverLatField)?.toDoubleOrNull()
      ?: return null

    val uploadedOn = data.get(uploadedOnField)?.toLongOrNull()
      ?: return null

    return PhotoExchangedData(
      uploadedPhotoName,
      receivedPhotoName,
      receiverLon,
      receiverLat,
      uploadedOn
    )
  }

  private fun sendPhotoReceivedBroadcast() {
    Timber.tag(TAG).d("sendPhotoReceivedBroadcast called")

    val intent = Intent().apply {
      action = PhotosActivity.newPhotoReceivedAction
    }

    sendBroadcast(intent)
  }

  private fun showNotification() {
    Timber.tag(TAG).d("showNotification called")

    val backIntent = Intent(this, TakePhotoActivity::class.java).apply {
      addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }

    val intent = Intent(this, PhotosActivity::class.java).apply {
      putExtra(PhotosActivity.extraNewPhotoReceived, true)
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

  data class PhotoExchangedData(
    val uploadedPhotoName: String,
    val receivedPhotoName: String,
    val lon: Double,
    val lat: Double,
    val uploadedOn: Long
  )

  companion object {
    private val vibrationPattern = LongArray(4).apply {
      this[0] = 0L
      this[1] = 300L
      this[2] = 200L
      this[3] = 300L
    }

    const val uploadedPhotoNameField = "uploaded_photo_name"
    const val receivedPhotoNameField = "received_photo_name"
    const val receiverLonField = "lon"
    const val receiverLatField = "lat"
    const val uploadedOnField = "uploaded_on"
    const val NOTIFICATION_ID = 3
  }
}