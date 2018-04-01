package com.kirakishou.photoexchange.ui.dialog

import android.content.Context
import android.support.v4.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.kirakishou.fixmypc.photoexchange.R
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 1/26/2018.
 */
class AppCannotWorkWithoutCameraPermissionDialog : AbstractDialog<Unit>() {
    override fun show(context: Context,
                      onPositiveCallback: (() -> Unit)?,
                      onNegativeCallback: (() -> Unit)?) {
        checkNotNull(onPositiveCallback)

        //TODO: change this to homemade dialog and get rid of the MaterialDialogs dependency
        MaterialDialog.Builder(context)
            .title("Error")
            .content("This app cannon work without a camera permission")
            .cancelable(false)
            .positiveColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .positiveText("OK")
            .onPositive { _, _ ->
                onPositiveCallback?.invoke()
            }
            .show()
    }
}