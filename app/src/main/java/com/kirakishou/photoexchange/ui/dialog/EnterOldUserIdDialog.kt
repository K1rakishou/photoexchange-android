package com.kirakishou.photoexchange.ui.dialog

import android.content.Context
import android.support.v4.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.kirakishou.fixmypc.photoexchange.R
import io.reactivex.Single

class EnterOldUserIdDialog {
    fun show(context: Context): Single<String> {
        return Single.create<String> { emitter ->
            MaterialDialog.Builder(context)
                .title("User id is required")
                .content("Please enter old user id to restore an account. This action will remove all your current photos")
                .input("1234567890abcdef@photoexchange.io", null) { _, input ->
                    emitter.onSuccess(input.toString())
                }
                .negativeColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .negativeText("Cancel")
                .onNegative { _, _ ->
                    emitter.onSuccess("")
                }
                .positiveColor(ContextCompat.getColor(context, R.color.colorAccent))
                .positiveText("Restore")
                .onPositive { _, _ ->
                }
                .show()
        }
    }
}