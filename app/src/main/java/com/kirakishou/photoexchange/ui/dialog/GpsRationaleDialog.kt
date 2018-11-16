package com.kirakishou.photoexchange.ui.dialog

import android.content.Context
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.kirakishou.fixmypc.photoexchange.R

/**
 * Created by kirakishou on 3/17/2018.
 */
class GpsRationaleDialog : AbstractDialog<Unit>() {
  override fun show(context: Context,
                    onPositiveCallback: (() -> Unit)?,
                    onNegativeCallback: (() -> Unit)?) {
    checkNotNull(onPositiveCallback)
    checkNotNull(onNegativeCallback)

    //TODO: change this to homemade dialog and get rid of the MaterialDialogs dependency
    MaterialDialog(context)
      .title(text = "Why do we need gps permission?")
      .message(text = "We need gps permission so other people can see where the photo was taken from. " +
        "But you can safely disable gps and all photos will be sent without the location")
      .negativeButton(text = "Do not allow") {
        onNegativeCallback.invoke()
      }
      .positiveButton(text = "Allow") {
        onPositiveCallback.invoke()
      }
      .show()
  }
}