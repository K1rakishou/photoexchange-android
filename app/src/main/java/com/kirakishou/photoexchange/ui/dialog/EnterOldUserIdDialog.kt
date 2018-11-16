package com.kirakishou.photoexchange.ui.dialog

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import io.reactivex.Single

class EnterOldUserIdDialog {
  fun show(context: Context): Single<String> {
    return Single.create<String> { emitter ->
      MaterialDialog(context)
        .title(text = "User id is required")
        .message(text = "Please enter old user id to restore an account. This action will remove all your current photos")
        .input("1234567890abcdef@photoexchange.io", null) { _, input ->
          emitter.onSuccess(input.toString())
        }
        .negativeButton(text = "Cancel") {
          emitter.onSuccess("")
        }
        .positiveButton(text = "Restore") {
        }
        .show()
    }
  }
}