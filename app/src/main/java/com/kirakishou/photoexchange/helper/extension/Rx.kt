package com.kirakishou.photoexchange.helper.extension

import com.kirakishou.photoexchange.interactors.UseCaseResult
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

/**
 * Created by kirakishou on 1/3/2018.
 */

fun <T> Observable<T>.debounceClicks(): Observable<T> {
    return this.observeOn(Schedulers.io())
        .throttleFirst(500, TimeUnit.MILLISECONDS)
        .observeOn(AndroidSchedulers.mainThread())
}

inline fun <T, reified R : UseCaseResult.Result<T>> Observable<UseCaseResult<T>>.drainErrorCodesTo(observer: Observer<ErrorCode>): Observable<T> {
    return this.map { useCaseResult ->
        if (useCaseResult !is R) {
            observer.onNext((useCaseResult as UseCaseResult.Error).errorCode)
        }

        return@map useCaseResult
    }
        .filter { it is R }
        .map { (it as R).result }
}