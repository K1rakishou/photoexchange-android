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
class AppCannotWorkWithoutCameraPermissionDialog  {

  fun show(context: Context, onPositiveCallback: (() -> Unit)) {
    MaterialDialog(context)
      .title(text = context.getString(R.string.app_cannot_work_without_camera_dialog_error))
      .message(text = context.getString(R.string.app_cannot_work_without_camera_dialog_app_cannot_work_without_camera))
      .cancelable(false)
      .positiveButton(text = context.getString(R.string.app_cannot_work_without_camera_dialog_ok)) {
        onPositiveCallback.invoke()
      }
      .show()
  }

}