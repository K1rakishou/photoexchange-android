package com.kirakishou.photoexchange.ui.dialog

import android.content.Context
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.kirakishou.fixmypc.photoexchange.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 1/26/2018.
 */
class CameraRationaleDialog(
  private val coroutineScope: CoroutineScope
) : AbstractDialog<Unit>() {
  override suspend fun show(context: Context,
                    onPositiveCallback: (suspend () -> Unit)?,
                    onNegativeCallback: (suspend () -> Unit)?) {
    checkNotNull(onPositiveCallback)
    checkNotNull(onNegativeCallback)

    MaterialDialog(context)
      .title(text = "Why do we need camera permission?")
      .message(text = "We need camera permission so you can take a photo that will be sent to someone else. This app cannot work without the camera permission")
      .cancelable(false)
      .negativeButton(text = "Close app") {
        coroutineScope.launch { onNegativeCallback.invoke() }
      }
      .positiveButton(text = "Allow") {
        coroutineScope.launch { onPositiveCallback.invoke() }
      }
      .show()
  }
}