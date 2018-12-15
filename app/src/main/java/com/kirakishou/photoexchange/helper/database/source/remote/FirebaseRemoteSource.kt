package com.kirakishou.photoexchange.helper.database.source.remote

import com.google.firebase.iid.FirebaseInstanceId
import com.kirakishou.photoexchange.helper.Constants
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FirebaseRemoteSource(
  private val firebaseInstanceId: FirebaseInstanceId
) {

  suspend fun getTokenAsync(): String {
    return suspendCoroutine { continuation ->
      firebaseInstanceId.instanceId.addOnCompleteListener { task ->
        if (!task.isSuccessful) {
          /**
           * User may not have google play services installed, so in this case we will use the default token
           * and all users with such token won't receive any push notifications
           * */

          continuation.resume(Constants.NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN)
          return@addOnCompleteListener
        }

        if (task.result == null) {
          continuation.resume(Constants.NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN)
        } else {
          continuation.resume(task.result!!.token)
        }
      }
    }
  }
}