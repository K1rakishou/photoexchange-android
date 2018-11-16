package com.kirakishou.photoexchange.ui.dialog

import android.content.Context
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.kirakishou.fixmypc.photoexchange.R
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers

class CancelPhotoUploadingDialog : AbstractDialog<Observable<Boolean>>() {

  override fun show(context: Context, onPositiveCallback: (() -> Unit)?, onNegativeCallback: (() -> Unit)?): Observable<Boolean> {
    val observable = Observable.create<Boolean> { emitter ->
      //TODO: change this to homemade dialog and get rid of the MaterialDialogs dependency
      MaterialDialog(context)
        .title(text = "Are you sure?")
        .message(text = "Do you really want to CANCEL uploading and DELETE this photo? This operation cannot be undone.")
        .negativeButton(text = "NO") {
          emitter.onNext(false)
          emitter.onComplete()
        }
        .positiveButton(text = "YES, CANCEL AND DELETE") {
          emitter.onNext(true)
          emitter.onComplete()
        }
        .show()
    }

    return observable
      .subscribeOn(AndroidSchedulers.mainThread())
  }
}