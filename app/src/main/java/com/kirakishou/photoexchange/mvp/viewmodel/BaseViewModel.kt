package com.kirakishou.photoexchange.mvp.viewmodel

import android.arch.lifecycle.ViewModel
import android.support.annotation.CallSuper
import io.reactivex.disposables.CompositeDisposable
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 9/8/2017.
 */
abstract class BaseViewModel<T> : ViewModel() {
    protected val compositeDisposable = CompositeDisposable()
    private var viewCallbacks: T? = null

    fun setView(view: T) {
        this.viewCallbacks = view
    }

    fun clearView() {
        this.viewCallbacks = null
    }

    protected fun getView(): T? {
        return viewCallbacks
    }

    @CallSuper
    override fun onCleared() {
        compositeDisposable.clear()

        super.onCleared()
    }
}