package com.kirakishou.photoexchange.helper.preference

import android.content.SharedPreferences
import javax.inject.Inject

/**
 * Created by kirakishou on 7/25/2017.
 */
class AppSharedPreference
@Inject constructor(protected val sharedPreferences: SharedPreferences) {

  inline fun <reified T : BasePreference> prepare(): T {
    return accessPrepare(T::class.java)
  }

  @Suppress("UNCHECKED_CAST")
  @PublishedApi
  internal fun <T : BasePreference> accessPrepare(clazz: Class<*>): T {
    return when (clazz) {
      else -> throw IllegalArgumentException("Unknown type T: ${clazz.simpleName}")
    }
  }
}