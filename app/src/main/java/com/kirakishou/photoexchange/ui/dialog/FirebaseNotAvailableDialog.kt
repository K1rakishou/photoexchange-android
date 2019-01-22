package com.kirakishou.photoexchange.ui.dialog

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import com.kirakishou.fixmypc.photoexchange.R
import java.lang.ref.WeakReference

class FirebaseNotAvailableDialog {

  fun show(context: Context, onPositiveCallback: WeakReference<(() -> Unit)>) {
    MaterialDialog(context)
      .title(text = context.getString(R.string.firebase_not_available_dialog_firebase_is_not_available))
      .message(text = context.getString(R.string.firebase_not_available_dialog_no_google_play_services))
      .positiveButton(text = context.getString(R.string.firebase_not_available_dialog_ok)) {
        onPositiveCallback.get()?.invoke()
      }
      .show()
  }

}