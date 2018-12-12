package com.kirakishou.photoexchange.mvp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.annotation.CallSuper
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlin.coroutines.CoroutineContext

/**
 * Created by kirakishou on 9/8/2017.
 */
abstract class BaseViewModel(
  private val dispatchersProvider: DispatchersProvider
) : ViewModel(), CoroutineScope {
  protected val compositeDisposable = CompositeDisposable()
  private val job = Job()

  override val coroutineContext: CoroutineContext
    get() = job + dispatchersProvider.GENERAL()

  @CallSuper
  override fun onCleared() {
    compositeDisposable.clear()
    job.cancelChildren()

    super.onCleared()
  }
}