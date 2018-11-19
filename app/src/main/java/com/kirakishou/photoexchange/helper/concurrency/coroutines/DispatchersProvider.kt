package com.kirakishou.photoexchange.helper.concurrency.coroutines

import kotlinx.coroutines.CoroutineDispatcher

interface DispatchersProvider {
  fun DB(): CoroutineDispatcher
  fun NETWORK(): CoroutineDispatcher
  fun GENERAL(): CoroutineDispatcher
  fun UI(): CoroutineDispatcher
}