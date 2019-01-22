package com.kirakishou.photoexchange.ui.dialog

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import com.kirakishou.fixmypc.photoexchange.R
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 3/17/2018.
 */
class GpsRationaleDialog {

  fun show(context: Context, onPositiveCallback: WeakReference<(() -> Unit)>, onNegativeCallback: WeakReference<(() -> Unit)>) {
    MaterialDialog(context)
      .title(text = context.getString(R.string.gps_rationale_dialog_why_do_we_need_gps_permission))
      .message(text = context.getString(R.string.gps_rationale_dialog_rationale_description))
      .negativeButton(text = context.getString(R.string.gps_rationale_dialog_rationale_do_not_allow)) {
        onNegativeCallback.get()?.invoke()
      }
      .positiveButton(text = context.getString(R.string.gps_rationale_dialog_rationale_allow)) {
        onPositiveCallback.get()?.invoke()
      }
      .show()
  }
}