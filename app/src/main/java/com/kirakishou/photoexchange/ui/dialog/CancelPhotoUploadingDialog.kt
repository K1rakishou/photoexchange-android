package com.kirakishou.photoexchange.ui.dialog

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import kotlinx.coroutines.CompletableDeferred

class CancelPhotoUploadingDialog : AbstractDialog<CompletableDeferred<Boolean>>() {

  override suspend fun show(
    context: Context,
    onPositiveCallback: (suspend () -> Unit)?,
    onNegativeCallback: (suspend () -> Unit)?
  ): CompletableDeferred<Boolean> {
    val completableDeferred = CompletableDeferred<Boolean>()

    MaterialDialog(context)
      .title(text = "Are you sure?")
      .message(text = "Do you really want to CANCEL uploading and DELETE this photo? This operation cannot be undone.")
      .negativeButton(text = "NO") {
        completableDeferred.complete(false)
      }
      .positiveButton(text = "YES, CANCEL AND DELETE") {
        completableDeferred.complete(true)
      }
      .show()

    return completableDeferred
  }
}