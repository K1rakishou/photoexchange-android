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
class AppCannotWorkWithoutCameraPermissionDialog(
  private val coroutineScope: CoroutineScope
) : AbstractDialog<Unit>() {
  override suspend fun show(context: Context,
                    onPositiveCallback: (suspend () -> Unit)?,
                    onNegativeCallback: (suspend () -> Unit)?) {
    checkNotNull(onPositiveCallback)

    MaterialDialog(context)
      .title(text = "Error")
      .message(text = "This app cannon work without a camera permission")
      .cancelable(false)
      .positiveButton(text = "OK") {
        coroutineScope.launch { onPositiveCallback.invoke() }
      }
      .show()
  }
}