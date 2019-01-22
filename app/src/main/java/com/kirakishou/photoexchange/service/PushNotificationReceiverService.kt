package com.kirakishou.photoexchange.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.helper.Constants
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.mvrx.model.NewReceivedPhoto
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.usecases.StorePhotoFromPushNotificationUseCase
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject


class PushNotificationReceiverService : FirebaseMessagingService() {
  private val TAG = "PushNotificationReceiverService"

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

    val newReceivedPhoto = try {
      extractData(remoteMessage)
    } catch (error: Throwable) {
      Timber.tag(TAG).e(error)
      null
    }

    if (newReceivedPhoto != null) {
      Timber.tag(TAG).d("Got new photo from someone")

      runBlocking {
        if (!storePhotoFromPushNotificationUseCase.storePhotoFromPushNotification(newReceivedPhoto)) {
          Timber.tag(TAG).w("Could not store newReceivedPhoto")
          return@runBlocking
        }

        showNotification(newReceivedPhoto)
        sendPhotoReceivedBroadcast(newReceivedPhoto)
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

  private fun extractData(remoteMessage: RemoteMessage): NewReceivedPhoto {
    val data = remoteMessage.data

    val uploadedPhotoName = data.get(NewReceivedPhoto.uploadedPhotoNameField)
    if (uploadedPhotoName.isNullOrEmpty()) {
      throw PushNotificationExctractionException("Could not extract uploadedPhotoName")
    }

    val receivedPhotoName = data.get(NewReceivedPhoto.receivedPhotoNameField)
    if (receivedPhotoName.isNullOrEmpty()) {
      throw PushNotificationExctractionException("Could not extract receivedPhotoName")
    }

    val receiverLon = data.get(NewReceivedPhoto.receiverLonField)?.toDoubleOrNull()
    if (receiverLon == null) {
      throw PushNotificationExctractionException("Could not extract receiverLon")
    }

    val receiverLat = data.get(NewReceivedPhoto.receiverLatField)?.toDoubleOrNull()
    if (receiverLat == null) {
      throw PushNotificationExctractionException("Could not extract receiverLat")
    }

    val uploadedOn = data.get(NewReceivedPhoto.uploadedOnField)?.toLongOrNull()
    if (uploadedOn == null) {
      throw PushNotificationExctractionException("Could not extract uploadedOn")
    }

    return NewReceivedPhoto(
      uploadedPhotoName,
      receivedPhotoName,
      receiverLon,
      receiverLat,
      uploadedOn
    )
  }

  private fun sendPhotoReceivedBroadcast(newReceivedPhoto: NewReceivedPhoto) {
    Timber.tag(TAG).d("sendPhotoReceivedBroadcast called")

    val intent = Intent().apply {
      action = PhotosActivity.newPhotoReceivedAction

      val bundle = Bundle()
      newReceivedPhoto.toBundle(bundle)

      putExtra(PhotosActivity.receivedPhotoExtra, bundle)
    }

    sendBroadcast(intent)
  }

  private fun showNotification(newReceivedPhoto: NewReceivedPhoto) {
    Timber.tag(TAG).d("showNotification called")

    val intent = Intent(this, PhotosActivity::class.java).apply {
      val bundle = Bundle()
      newReceivedPhoto.toBundle(bundle)

      putExtra(PhotosActivity.receivedPhotoExtra, bundle)
    }

    val pendingIntent = PendingIntent.getActivity(
      this,
      0,
      intent,
      PendingIntent.FLAG_ONE_SHOT
    )

    val notificationBuilder = NotificationCompat.Builder(this, Constants.CHANNEL_ID)
      .setSmallIcon(android.R.drawable.stat_sys_download_done)
      .setContentTitle("You got a new photo from someone")
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)

    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        Constants.CHANNEL_ID,
        Constants.CHANNEL_NAME,
        NotificationManager.IMPORTANCE_DEFAULT
      )

      channel.enableLights(true)
      channel.vibrationPattern = vibrationPattern

      notificationManager.createNotificationChannel(channel)
    }

    notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
  }

  class PushNotificationExctractionException(msg: String) : Exception(msg)

  companion object {
    private val vibrationPattern = LongArray(4).apply {
      this[0] = 0L
      this[1] = 300L
      this[2] = 200L
      this[3] = 300L
    }

    const val NOTIFICATION_ID = 3
  }
}