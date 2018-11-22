package com.kirakishou.photoexchange.ui.dialog

import android.content.Context
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 1/26/2018.
 */
abstract class AbstractDialog<T> {
  abstract suspend fun show(context: Context,
                    onPositiveCallback: (suspend () -> Unit)? = null,
                    onNegativeCallback: (suspend () -> Unit)? = null): T
}