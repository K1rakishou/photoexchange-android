package com.kirakishou.photoexchange.ui.dialog

import android.content.Context
import android.support.v4.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.kirakishou.fixmypc.photoexchange.R

/**
 * Created by kirakishou on 3/17/2018.
 */
class GpsRationaleDialog : AbstractDialog<Unit>() {
    override fun show(context: Context,
                      onPositiveCallback: (() -> Unit)?,
                      onNegativeCallback: (() -> Unit)?) {
        checkNotNull(onPositiveCallback)
        checkNotNull(onNegativeCallback)

        //TODO: change this to homemade dialog and get rid of the MaterialDialogs dependency
        MaterialDialog.Builder(context)
            .title("Why do we need gps permission?")
            .content("We need gps permission so other people can see where the photo was taken from. " +
                "But you can safely disable gps and all photos will be sent without the location")
            .negativeColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .negativeText("Do not allow")
            .onNegative { _, _ ->
                onNegativeCallback?.invoke()
            }
            .positiveColor(ContextCompat.getColor(context, R.color.colorAccent))
            .onPositive { _, _ ->
                onPositiveCallback?.invoke()
            }
            .positiveText("Allow")
            .show()
    }
}