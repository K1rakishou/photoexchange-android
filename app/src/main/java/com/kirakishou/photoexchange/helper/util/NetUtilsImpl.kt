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
  private val context: Context,
  private val settingsRepository: SettingsRepository
) : NetUtils {
  override fun canLoadImages(): Boolean {
    if (!isNetworkMetered()) {
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
    if (!isNetworkMetered()) {
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

  private fun isNetworkMetered(): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
      as ConnectivityManager

    return connectivityManager.isActiveNetworkMetered
  }
}