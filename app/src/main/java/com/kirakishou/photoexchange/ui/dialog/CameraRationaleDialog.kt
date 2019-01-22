package com.kirakishou.photoexchange.ui.dialog

import android.content.Context
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.kirakishou.fixmypc.photoexchange.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 1/26/2018.
 */
class CameraRationaleDialog {

  fun show(context: Context, onPositiveCallback: WeakReference<(() -> Unit)>, onNegativeCallback: WeakReference<(() -> Unit)>) {
    MaterialDialog(context)
      .title(text = context.getString(R.string.camera_rationale_dialog_why_do_we_need_camera_permission))
      .message(text = context.getString(R.string.camera_rationale_dialog_rationale_description))
      .cancelable(false)
      .negativeButton(text = context.getString(R.string.camera_rationale_dialog_close_app)) {
        onNegativeCallback.get()?.invoke()
      }
      .positiveButton(text = context.getString(R.string.camera_rationale_dialog_allow)) {
        onPositiveCallback.get()?.invoke()
      }
      .show()
  }

}