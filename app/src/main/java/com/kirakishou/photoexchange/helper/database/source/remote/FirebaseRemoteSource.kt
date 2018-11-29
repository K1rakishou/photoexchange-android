package com.kirakishou.photoexchange.helper.database.source.remote

import com.google.firebase.iid.FirebaseInstanceId
import com.kirakishou.photoexchange.helper.Constants
import kotlinx.coroutines.CompletableDeferred

class FirebaseRemoteSource(
  private val firebaseInstanceId: FirebaseInstanceId
) {

  fun getTokenAsync(): CompletableDeferred<String?> {
    val result = CompletableDeferred<String?>()

    /**
     * User may not have google play services installed, so in this we will use this default token
     * and all users with this token won't receive any push notifications
     * */
    if (!isGoogleServicesAvailable()) {
      result.complete(Constants.NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN)
    } else {
      firebaseInstanceId.instanceId.addOnCompleteListener { task ->
        if (!task.isSuccessful) {
          result.completeExceptionally(task.exception!!)
          return@addOnCompleteListener
        }

        result.complete(task.result?.token)
      }
    }

    return result
  }

  private fun isGoogleServicesAvailable(): Boolean {
    //TODO:
    return true
  }
}