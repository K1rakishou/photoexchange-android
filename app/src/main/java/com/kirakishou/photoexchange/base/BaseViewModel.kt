package com.kirakishou.photoexchange.base

import android.arch.lifecycle.ViewModel
import android.support.annotation.CallSuper
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.helper.CompositeJob
import io.reactivex.disposables.CompositeDisposable

/**
 * Created by kirakishou on 9/8/2017.
 */
abstract class BaseViewModel : ViewModel() {
    protected val compositeDisposable = CompositeDisposable()
    protected val compositeJob = CompositeJob()

    @CallSuper
    override fun onCleared() {
        compositeDisposable.clear()
        compositeJob.cancelAll()

        PhotoExchangeApplication.refWatcher.watch(this, this::class.simpleName)
        super.onCleared()
    }
}