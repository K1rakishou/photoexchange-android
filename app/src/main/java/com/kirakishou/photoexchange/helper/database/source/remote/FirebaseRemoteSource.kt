package com.kirakishou.photoexchange.helper.database.source.remote

import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.coroutines.CompletableDeferred

class FirebaseRemoteSource(
  private val firebaseInstanceId: FirebaseInstanceId
) {

  fun getTokenAsync(): CompletableDeferred<String?> {
    val result = CompletableDeferred<String?>()

    firebaseInstanceId.instanceId.addOnCompleteListener { task ->
      if (!task.isSuccessful) {
        result.completeExceptionally(task.exception!!)
        return@addOnCompleteListener
      }

      result.complete(task.result?.token)
    }

    return result
  }
}