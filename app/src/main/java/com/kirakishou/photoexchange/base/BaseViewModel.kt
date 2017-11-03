package com.kirakishou.photoexchange.base

import android.arch.lifecycle.ViewModel
import android.support.annotation.CallSuper
import io.reactivex.disposables.CompositeDisposable

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