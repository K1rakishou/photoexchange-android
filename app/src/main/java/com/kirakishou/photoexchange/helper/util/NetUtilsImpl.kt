package com.kirakishou.photoexchange.helper.util

import android.content.Context
import android.net.ConnectivityManager


/**
 * Created by kirakishou on 7/26/2017.
 */
class NetUtilsImpl(
  context: Context
) : NetUtils {
  private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

  //TODO: add support for api23+
  override fun allowedToAccessNetwork(): Boolean {
    val activeNetworkInfo = connectivityManager.activeNetworkInfo
      ?: return false

    val isWiFi = activeNetworkInfo.type == ConnectivityManager.TYPE_WIFI
    return activeNetworkInfo.isConnectedOrConnecting && isWiFi
  }
}