package com.kirakishou.photoexchange.ui.dialog

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import com.kirakishou.fixmypc.photoexchange.R

/**
 * Created by kirakishou on 1/26/2018.
 */
class DeletePhotoConfirmationDialog {

  fun show(context: Context, onPositiveCallback: () -> Unit) {
    MaterialDialog(context)
      .title(text = context.getString(R.string.delete_photo_confirmation_dialog_confirmation_required))
      .message(text = context.getString(R.string.delete_photo_confirmation_dialog_would_you_like_to_delete_photo))
      .negativeButton(text = context.getString(R.string.delete_photo_confirmation_dialog_do_not_delete)) {
      }
      .positiveButton(text = context.getString(R.string.delete_photo_confirmation_dialog_delete)) {
        onPositiveCallback.invoke()
      }
      .show()
  }

}