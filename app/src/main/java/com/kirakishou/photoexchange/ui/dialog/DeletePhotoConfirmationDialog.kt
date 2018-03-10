package com.kirakishou.photoexchange.ui.dialog

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 1/26/2018.
 */
class DeletePhotoConfirmationDialog : AbstractDialog() {
    override fun show(context: Context,
                      onPositiveCallback: WeakReference<() -> Unit>?,
                      onNegativeCallback: WeakReference<() -> Unit>?) {
        checkNotNull(onPositiveCallback)

        //TODO: change this to homemade dialog and get rid of the MaterialDialogs dependency
        MaterialDialog.Builder(context)
                .title("Confirmation required")
                .content("Are you sure you want to delete this photo?")
                .positiveText("Delete")
                .negativeText("Cancel")
                .onPositive { _, _ ->
                    onPositiveCallback?.get()?.invoke()
                }
                .onNegative { _, _ ->
                }
                .show()
    }
}