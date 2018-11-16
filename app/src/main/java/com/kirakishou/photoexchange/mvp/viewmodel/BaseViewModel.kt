package com.kirakishou.photoexchange.mvp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.annotation.CallSuper
import io.reactivex.disposables.CompositeDisposable
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 9/8/2017.
 */
abstract class BaseViewModel : ViewModel() {
  protected val compositeDisposable = CompositeDisposable()

  @CallSuper
  override fun onCleared() {
    compositeDisposable.clear()

    super.onCleared()
  }
}