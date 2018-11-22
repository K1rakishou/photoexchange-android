package com.kirakishou.photoexchange.ui.dialog

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Created by kirakishou on 3/17/2018.
 */
class GpsRationaleDialog(
  private val coroutineScope: CoroutineScope
) : AbstractDialog<Unit>() {

  override suspend fun show(context: Context,
                            onPositiveCallback: (suspend () -> Unit)?,
                            onNegativeCallback: (suspend () -> Unit)?) {
    checkNotNull(onPositiveCallback)
    checkNotNull(onNegativeCallback)

    //TODO: change this to homemade dialog and get rid of the MaterialDialogs dependency
    MaterialDialog(context)
      .title(text = "Why do we need gps permission?")
      .message(text = "We need gps permission so other people can see where the photo was taken from. " +
        "But you can safely disable gps and all photos will be sent without the location")
      .negativeButton(text = "Do not allow") {
        coroutineScope.launch { onNegativeCallback.invoke() }
      }
      .positiveButton(text = "Allow") {
        coroutineScope.launch { onPositiveCallback.invoke() }
      }
      .show()
  }
}