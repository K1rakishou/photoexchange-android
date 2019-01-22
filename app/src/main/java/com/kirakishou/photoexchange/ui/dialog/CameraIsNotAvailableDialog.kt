package com.kirakishou.photoexchange.ui.dialog

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import com.kirakishou.fixmypc.photoexchange.R

/**
 * Created by kirakishou on 1/26/2018.
 */
class CameraIsNotAvailableDialog {

  fun show(context: Context, onPositiveCallback: () -> Unit) {
    MaterialDialog(context)
      .title(text = context.getString(R.string.camera_is_not_available_dialog_camera_is_not_available))
      .message(text = context.getString(R.string.camera_is_not_available_dialog_app_cannot_work_without_camera))
      .cancelable(false)
      .positiveButton(text = context.getString(R.string.camera_is_not_available_dialog_ok)) {
        onPositiveCallback.invoke()
      }
      .show()
  }

}