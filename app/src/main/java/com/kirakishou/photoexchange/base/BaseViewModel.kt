package com.kirakishou.photoexchange.base

import android.arch.lifecycle.ViewModel
import android.support.annotation.CallSuper
import io.reactivex.disposables.CompositeDisposable
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 9/8/2017.
 */
abstract class BaseViewModel<out T>(
    _view: T
) : ViewModel() {
    protected val compositeDisposable = CompositeDisposable()
    private var view: T? = _view

    protected fun getView(): T? {
        return view
    }

    protected fun clearView() {
        view = null
    }

    abstract fun init()
    abstract fun tearDown()

    @CallSuper
    override fun onCleared() {
        compositeDisposable.clear()

        super.onCleared()
    }
}