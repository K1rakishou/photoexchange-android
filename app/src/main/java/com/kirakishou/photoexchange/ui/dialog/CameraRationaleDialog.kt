package com.kirakishou.photoexchange.ui.dialog

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 1/26/2018.
 */
class CameraRationaleDialog : AbstractDialog() {
    override fun show(context: Context,
                      onPositiveCallback: (() -> Unit)?,
                      onNegativeCallback: (() -> Unit)?) {
        checkNotNull(onPositiveCallback)
        checkNotNull(onNegativeCallback)

        //TODO: change this to homemade dialog and get rid of the MaterialDialogs dependency
        MaterialDialog.Builder(context)
                .title("Why do we need camera permission?")
                .content("We need camera permission so you can take a photo that will be sent to someone else.")
                .positiveText("Allow")
                .negativeText("Close app")
                .onPositive { _, _ ->
                    onPositiveCallback?.invoke()
                }
                .onNegative { _, _ ->
                    onNegativeCallback?.invoke()
                }
                .show()
    }
}