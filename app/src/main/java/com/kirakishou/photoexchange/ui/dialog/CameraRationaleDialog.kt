package com.kirakishou.photoexchange.ui.dialog

import android.content.Context
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.kirakishou.fixmypc.photoexchange.R
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 1/26/2018.
 */
class CameraRationaleDialog : AbstractDialog<Unit>() {
  override fun show(context: Context,
                    onPositiveCallback: (() -> Unit)?,
                    onNegativeCallback: (() -> Unit)?) {
    checkNotNull(onPositiveCallback)
    checkNotNull(onNegativeCallback)

    //TODO: change this to homemade dialog and get rid of the MaterialDialogs dependency
    MaterialDialog(context)
      .title(text = "Why do we need camera permission?")
      .message(text = "We need camera permission so you can take a photo that will be sent to someone else.")
      .cancelable(false)
      .negativeButton(text = "Close app") {
        onNegativeCallback.invoke()
      }
      .positiveButton(text = "Allow") {
        onPositiveCallback.invoke()
      }
      .show()
  }
}