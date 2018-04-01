package com.kirakishou.photoexchange.ui.dialog

import android.content.Context
import android.support.v4.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.kirakishou.fixmypc.photoexchange.R
import io.reactivex.Observable

class CancelAllQueuedUpPhotosDialog : AbstractDialog<Observable<Boolean>>() {

    override fun show(context: Context, onPositiveCallback: (() -> Unit)?, onNegativeCallback: (() -> Unit)?): Observable<Boolean> {
        return Observable.create { emitter ->
            //TODO: change this to homemade dialog and get rid of the MaterialDialogs dependency
            MaterialDialog.Builder(context)
                .title("Are you sure?")
                .content("Do you really want to CANCEL uploading and DELETE all queued up photos? This operation cannot be undone.")
                .negativeText("NO")
                .negativeColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .onNegative { _, _ ->
                    emitter.onNext(false)
                    emitter.onComplete()
                }
                .positiveColor(ContextCompat.getColor(context, R.color.colorAccent))
                .positiveText("YES, CANCEL AND DELETE")
                .onPositive { _, _ ->
                    emitter.onNext(true)
                    emitter.onComplete()
                }
                .show()
        }
    }
}