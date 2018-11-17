package com.kirakishou.photoexchange.helper.concurrency.coroutines

import kotlinx.coroutines.CoroutineDispatcher

interface DispatchersProvider {
  fun IO(): CoroutineDispatcher
  fun DISK(): CoroutineDispatcher
  fun GENERAL(): CoroutineDispatcher
  fun UI(): CoroutineDispatcher
}