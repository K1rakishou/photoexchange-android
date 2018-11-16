package com.kirakishou.photoexchange.ui.dialog

import android.content.Context
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.kirakishou.fixmypc.photoexchange.R
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 1/26/2018.
 */
class CameraIsNotAvailableDialog : AbstractDialog<Unit>() {
  override fun show(context: Context,
                    onPositiveCallback: (() -> Unit)?,
                    onNegativeCallback: (() -> Unit)?) {
    checkNotNull(onPositiveCallback)

    //TODO: change this to homemade dialog and get rid of the MaterialDialogs dependency
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