package com.kirakishou.photoexchange.ui.dialog

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Created by kirakishou on 1/26/2018.
 */
class CameraIsNotAvailableDialog {

  fun show(context: Context, onPositiveCallback: () -> Unit) {
    MaterialDialog(context)
      .title(text = "Camera is not available")
      .message(text = "It looks like your device does not support camera. This app cannot work without a camera.")
      .cancelable(false)
      .positiveButton(text = "OK") {
        onPositiveCallback.invoke()
      }
      .show()
  }
}