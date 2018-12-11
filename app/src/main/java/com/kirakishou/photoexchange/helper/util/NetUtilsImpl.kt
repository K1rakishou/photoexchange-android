package com.kirakishou.photoexchange.helper.util

import android.content.Context
import android.net.ConnectivityManager
import com.kirakishou.photoexchange.helper.NetworkAccessLevel
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext


/**
 * Created by kirakishou on 7/26/2017.
 */
class NetUtilsImpl(
  private val context: Context,
  private val settingsRepository: SettingsRepository,
  private val dispatchersProvider: DispatchersProvider
) : NetUtils, CoroutineScope {

  private val job = Job()

  override val coroutineContext: CoroutineContext
    get() = job + dispatchersProvider.GENERAL()

  override suspend fun canLoadImages(): Boolean {
    return withContext(coroutineContext) {
      if (!isNetworkMetered()) {
        return@withContext true
      }

      return@withContext when (settingsRepository.getNetworkAccessLevel()) {
        NetworkAccessLevel.Neither -> false
        NetworkAccessLevel.CanAccessInternet -> false
        NetworkAccessLevel.CanLoadImages -> true
      }
    }
  }

  override suspend fun canAccessNetwork(): Boolean {
    return withContext(coroutineContext) {
      if (!isNetworkMetered()) {
        return@withContext true
      }

      return@withContext when (settingsRepository.getNetworkAccessLevel()) {
        NetworkAccessLevel.Neither -> false
        NetworkAccessLevel.CanAccessInternet -> true
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