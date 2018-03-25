package com.kirakishou.photoexchange.base

import android.arch.lifecycle.ViewModel
import android.support.annotation.CallSuper
import io.reactivex.disposables.CompositeDisposable
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 9/8/2017.
 */
abstract class BaseViewModel<T>(
    _view: WeakReference<T>
) : ViewModel() {
    protected val compositeDisposable = CompositeDisposable()
    private var viewCallbacks = _view

    protected fun getView(): T? {
        return viewCallbacks.get()
    }

    abstract fun onAttached()

    @CallSuper
    override fun onCleared() {
        compositeDisposable.clear()

        super.onCleared()
    }
}