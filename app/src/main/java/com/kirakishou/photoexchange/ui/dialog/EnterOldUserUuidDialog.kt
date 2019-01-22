package com.kirakishou.photoexchange.ui.dialog

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.kirakishou.fixmypc.photoexchange.R
import java.lang.ref.WeakReference

class EnterOldUserUuidDialog {

  fun show(context: Context, onPositiveCallback: WeakReference<((String) -> Unit)>) {
    var text = ""

    MaterialDialog(context)
      .title(text = context.getString(R.string.enter_old_user_uuid_dialog_user_uuid_is_required))
      .message(text = context.getString(R.string.enter_old_user_uuid_dialog_please_enter_old_user_uuid))
      .input(context.getString(R.string.enter_old_user_uuid_dialog_user_uuid_placeholder), null) { _, input ->
        text = input.toString()
      }
      .negativeButton(text = context.getString(R.string.enter_old_user_uuid_dialog_cancel)) {
        onPositiveCallback.get()?.invoke("")
      }
      .positiveButton(text = context.getString(R.string.enter_old_user_uuid_dialog_restore)) {
        onPositiveCallback.get()?.invoke(text)
      }
      .show()
  }

}