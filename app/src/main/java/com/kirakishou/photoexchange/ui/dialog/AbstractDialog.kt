package com.kirakishou.photoexchange.ui.dialog

import android.content.Context

/**
 * Created by kirakishou on 1/26/2018.
 */
abstract class AbstractDialog {
    abstract fun show(context: Context, onPositiveCallback: (() -> Unit)? = null, onNegativeCallback: (() -> Unit)? = null)
}