package com.kirakishou.photoexchange.helper.concurrency.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.newSingleThreadContext

class NormalDispatchers : DispatchersProvider {
  override fun DB(): CoroutineDispatcher = newSingleThreadContext("database")
  override fun NETWORK(): CoroutineDispatcher = newFixedThreadPoolContext(Runtime.getRuntime().availableProcessors(), "network")
  override fun GENERAL(): CoroutineDispatcher = Dispatchers.Default
  override fun UI(): CoroutineDispatcher = Dispatchers.Main
}