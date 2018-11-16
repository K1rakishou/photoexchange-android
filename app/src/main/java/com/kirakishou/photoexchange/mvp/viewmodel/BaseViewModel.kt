package com.kirakishou.photoexchange.mvp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.annotation.CallSuper
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

/**
 * Created by kirakishou on 9/8/2017.
 */
abstract class BaseViewModel : ViewModel(), CoroutineScope {
  protected val compositeDisposable = CompositeDisposable()
  private val job = Job()

  override val coroutineContext: CoroutineContext
    get() = job

  @CallSuper
  override fun onCleared() {
    compositeDisposable.clear()
    job.cancel()

    super.onCleared()
  }
}