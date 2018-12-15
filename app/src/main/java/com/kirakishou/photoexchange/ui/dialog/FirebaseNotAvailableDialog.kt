package com.kirakishou.photoexchange.ui.dialog

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FirebaseNotAvailableDialog {

  suspend fun show(context: Context) {
    return suspendCoroutine<Unit> { continuation ->
      MaterialDialog(context)
        .title(text = "Firebase is not available")
        .message(text = "It appears that your phone has no Google Play Services installed. That's okay but you won't be able to receive push notifications.")
        .positiveButton(text = "Ok") {
          continuation.resume(Unit)
        }
        .show()
    }
  }

}