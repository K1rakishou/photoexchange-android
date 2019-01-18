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
open class NetUtilsImpl(
  private val context: Context,
  private val settingsRepository: SettingsRepository,
  private val dispatchersProvider: DispatchersProvider
) : NetUtils, CoroutineScope {

  private val job = Job()

  override val coroutineContext: CoroutineContext
    get() = job + dispatchersProvider.GENERAL()

  /**
   * Returns true when:
   * 1. Current network is unmetered (for example Wi-Fi connection)
   * 2. Current network is metered but user has allowed to load images with metered connection
   * */
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

  /**
   * Returns true when:
   * 1. Current network is unmetered (for example Wi-Fi connection)
   * 2. Current network is metered but user has allowed to access internet with metered connection
   * */
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