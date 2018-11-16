package com.kirakishou.photoexchange.helper.extension

import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 3/10/2018.
 */

fun <T> T.asWeak(): WeakReference<T> {
  return WeakReference(this)
}

val Any?.safe get() = Unit