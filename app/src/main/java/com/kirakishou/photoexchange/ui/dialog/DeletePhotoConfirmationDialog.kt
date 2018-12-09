package com.kirakishou.photoexchange.ui.dialog

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog

/**
 * Created by kirakishou on 1/26/2018.
 */
class DeletePhotoConfirmationDialog  {
  fun show(context: Context,
                    onPositiveCallback: () -> Unit) {
    MaterialDialog(context)
      .title(text = "Confirmation required")
      .message(text = "Would you like to delete this photo as well?")
      .negativeButton(text = "Cancel") {
      }
      .positiveButton(text = "Delete") {
        onPositiveCallback.invoke()
      }
      .show()
  }
}