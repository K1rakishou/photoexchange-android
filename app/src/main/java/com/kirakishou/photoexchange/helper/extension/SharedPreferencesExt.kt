package com.kirakishou.fixmypc.fixmypcapp.helper.extension

import android.content.SharedPreferences

/**
 * Created by kirakishou on 7/25/2017.
 */

inline fun SharedPreferences.edit(operation: (SharedPreferences.Editor) -> Unit) {
  val editor = this.edit()
  operation(editor)
  editor.apply()
}