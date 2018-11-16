package com.kirakishou.photoexchange.helper.extension

import android.app.Activity
import com.kirakishou.photoexchange.helper.util.AndroidUtils

/**
 * Created by kirakishou on 10/11/2017.
 */

fun Activity.hideKeyboard() {
  AndroidUtils.hideSoftKeyboard(this)
}