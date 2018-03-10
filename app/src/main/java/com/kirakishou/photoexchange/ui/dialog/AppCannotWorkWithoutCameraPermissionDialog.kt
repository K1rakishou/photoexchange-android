package com.kirakishou.photoexchange.ui.dialog

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 1/26/2018.
 */
class AppCannotWorkWithoutCameraPermissionDialog : AbstractDialog() {
    override fun show(context: Context,
                      onPositiveCallback: WeakReference<() -> Unit>?,
                      onNegativeCallback: WeakReference<() -> Unit>?) {
        checkNotNull(onPositiveCallback)

        //TODO: change this to homemade dialog and get rid of the MaterialDialogs dependency
        MaterialDialog.Builder(context)
                .title("Error")
                .content("This app cannon work without a camera permission")
                .positiveText("OK")
                .onPositive { _, _ ->
                    onPositiveCallback?.get()?.invoke()
                }
                .show()
    }
}