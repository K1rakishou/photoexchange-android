package com.kirakishou.photoexchange.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

class PushNotificationReceiverService : FirebaseMessagingService() {
  private val TAG = "PushNotificationReceiverService"

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

    //TODO: create notification
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
      } catch (error: Throwable) {
        Timber.tag(TAG).e(error, "Could not update new firebase token")
      }
    }
  }
}