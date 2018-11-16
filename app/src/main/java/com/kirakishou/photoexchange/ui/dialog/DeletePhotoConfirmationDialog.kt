package com.kirakishou.photoexchange.ui.dialog

import android.content.Context
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.kirakishou.fixmypc.photoexchange.R

/**
 * Created by kirakishou on 1/26/2018.
 */
class DeletePhotoConfirmationDialog : AbstractDialog<Unit>() {
  override fun show(context: Context,
                    onPositiveCallback: (() -> Unit)?,
                    onNegativeCallback: (() -> Unit)?) {
    checkNotNull(onPositiveCallback)

    //TODO: change this to homemade dialog and get rid of the MaterialDialogs dependency
    MaterialDialog(context)
      .title(text = "Confirmation required")
      .message(text = "Are you sure you want to delete this photo?")
      .negativeButton(text = "Cancel") {
      }
      .positiveButton(text = "Delete") {
        onPositiveCallback.invoke()
      }

      .show()
  }
}