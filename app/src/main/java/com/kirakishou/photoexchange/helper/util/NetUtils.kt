package com.kirakishou.photoexchange.helper.util

import android.content.Context
import android.net.ConnectivityManager

/**
 * Created by kirakishou on 7/26/2017.
 */
object NetUtils {

    fun isWifiConnected(context: Context): Boolean {
        val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

        return mWifi.isConnected
    }
}