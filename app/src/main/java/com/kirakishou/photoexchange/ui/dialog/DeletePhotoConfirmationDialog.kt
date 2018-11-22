package com.kirakishou.photoexchange.ui.dialog

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Created by kirakishou on 1/26/2018.
 */
class DeletePhotoConfirmationDialog(
  private val coroutineScope: CoroutineScope
) : AbstractDialog<Unit>() {
  override suspend fun show(context: Context,
                    onPositiveCallback: (suspend () -> Unit)?,
                    onNegativeCallback: (suspend () -> Unit)?) {
    checkNotNull(onPositiveCallback)

    //TODO: change this to homemade dialog and get rid of the MaterialDialogs dependency
    MaterialDialog(context)
      .title(text = "Confirmation required")
      .message(text = "Are you sure you want to delete this photo?")
      .negativeButton(text = "Cancel") {
      }
      .positiveButton(text = "Delete") {
        coroutineScope.launch { onPositiveCallback.invoke() }
      }

      .show()
  }
}