package com.kirakishou.photoexchange.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.exception.EmptyUserIdException
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.interactors.GetUserIdUseCase
import com.kirakishou.photoexchange.interactors.UpdateFirebaseTokenUseCase
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

class PushNotificationReceiverService : FirebaseMessagingService() {
  private val TAG = "PushNotificationReceiverService"

  @Inject
  lateinit var updateFirebaseTokenUseCase: UpdateFirebaseTokenUseCase

  @Inject
  lateinit var getUserIdUseCase: GetUserIdUseCase

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

  //TODO:
  // Probably will have to rewrite this because right now this method makes two http-requests and they may fail
  // or the user may try to upload photo before these two request have been completed.
  // What I need to do here is to just store the need token in the database, then upon uploading
  // I should check whether this token differs from the  other token (that is being stored in another
  // database column) and if they are - that means that we got new token and I should update token on the server.
  // if they are the same - then nothing need to be done.
  override fun onNewToken(token: String?) {
    Timber.tag(TAG).d("onNewToken called")

    if (token == null) {
      Timber.tag(TAG).w("Null token has been received!")
      return
    }

    runBlocking {
      try {
        //we need to get the userId first because this operation will create a default account on the server
        val userId = getUserId()
        if (userId.isEmpty()) {
          throw EmptyUserIdException()
        }

        updateFirebaseToken(token)
      } catch (error: Throwable) {
        Timber.tag(TAG).e(error, "Could not update firebase token")
      }
    }
  }

  private suspend fun updateFirebaseToken(newToken: String) {
    val result = updateFirebaseTokenUseCase.updateFirebaseToken(newToken)

    when (result) {
      is Either.Value -> Timber.tag(TAG).d("Successfully updated token")
      is Either.Error -> throw result.error
    }.safe
  }

  private suspend fun getUserId(): String {
    val result = getUserIdUseCase.getUserId()

    when (result) {
      is Either.Value -> return result.value
      is Either.Error -> throw result.error
    }.safe
  }
}