package com.kirakishou.photoexchange.helper.util

import android.content.Context
import android.net.ConnectivityManager
import com.kirakishou.photoexchange.helper.NetworkAccessLevel
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import kotlinx.coroutines.runBlocking


/**
 * Created by kirakishou on 7/26/2017.
 */
class NetUtilsImpl(
  context: Context,
  private val settingsRepository: SettingsRepository
) : NetUtils {
  private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

  override fun canLoadImages(): Boolean {
    if (isWiFiConnected()) {
      return true
    }

    return runBlocking {
      return@runBlocking when (settingsRepository.getNetworkAccessLevel()) {
        NetworkAccessLevel.Neither -> false
        NetworkAccessLevel.CanAccessInternet -> false
        NetworkAccessLevel.CanLoadImages -> true
      }
    }
  }

  override fun canAccessNetwork(): Boolean {
    if (isWiFiConnected()) {
      return true
    }

    return runBlocking {
      return@runBlocking when (settingsRepository.getNetworkAccessLevel()) {
        NetworkAccessLevel.Neither -> false
        NetworkAccessLevel.CanAccessInternet -> false
        NetworkAccessLevel.CanLoadImages -> true
      }
    }
  }

  //TODO: add support for api23+
  private fun isWiFiConnected(): Boolean {
    val activeNetworkInfo = connectivityManager.activeNetworkInfo
      ?: return false

    val isWiFi = activeNetworkInfo.type == ConnectivityManager.TYPE_WIFI
    return activeNetworkInfo.isConnectedOrConnecting && isWiFi
  }
}