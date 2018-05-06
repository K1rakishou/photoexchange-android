package com.kirakishou.photoexchange.helper.extension

import com.kirakishou.photoexchange.helper.Either
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

inline fun <E, V, reified R : Either.Value<V>> Observable<Either<E, V>>.drainErrorCodesTo(observer: Observer<E>): Observable<V> {
    return this.map { useCaseResult ->
        if (useCaseResult !is R) {
            observer.onNext((useCaseResult as Either.Error).error)
        }

        return@map useCaseResult
    }
        .filter { it is R }
        .map { (it as R).value }
}