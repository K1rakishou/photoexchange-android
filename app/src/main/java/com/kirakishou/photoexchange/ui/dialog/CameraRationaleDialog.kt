package com.kirakishou.photoexchange.ui.dialog

import android.content.Context
import android.support.v4.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.kirakishou.fixmypc.photoexchange.R
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 1/26/2018.
 */
class CameraRationaleDialog : AbstractDialog<Unit>() {
    override fun show(context: Context,
                      onPositiveCallback: (() -> Unit)?,
                      onNegativeCallback: (() -> Unit)?) {
        checkNotNull(onPositiveCallback)
        checkNotNull(onNegativeCallback)

        //TODO: change this to homemade dialog and get rid of the MaterialDialogs dependency
        MaterialDialog.Builder(context)
            .title("Why do we need camera permission?")
            .content("We need camera permission so you can take a photo that will be sent to someone else.")
            .cancelable(false)
            .negativeColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .negativeText("Close app")
            .onNegative { _, _ ->
                onNegativeCallback?.invoke()
            }
            .positiveColor(ContextCompat.getColor(context, R.color.colorAccent))
            .positiveText("Allow")
            .onPositive { _, _ ->
                onPositiveCallback?.invoke()
            }
            .show()
    }
}