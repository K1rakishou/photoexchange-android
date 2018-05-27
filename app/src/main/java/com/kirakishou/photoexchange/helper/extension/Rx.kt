package com.kirakishou.photoexchange.helper.extension

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.mvp.model.exception.ErrorCodeException
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

fun <T> Observable<Pair<Class<*>, T>>.filterErrorCodes(clazz: Class<*>): Observable<T> {
    return this
        .filter { it.first == clazz }
        .map { it.second }
}

fun <V> Observable<Either<ErrorCode, V>>.mapEither(): Observable<V> {
    return this.map {
        if (it is Either.Error) {
            throw ErrorCodeException(it.error)
        }

        return@map (it as Either.Value).value
    }
}

inline fun <E, V, reified R : Either.Value<V>> Observable<Either<E, V>>.drainErrorCodesTo(
    observer: Observer<Pair<Class<*>, E>>,
    vararg classes: Class<*>
): Observable<V> {
    return this.map { useCaseResult ->
        if (useCaseResult !is R) {
            for (clazz in classes) {
                val error = Pair(clazz, (useCaseResult as Either.Error).error)
                observer.onNext(error)
            }
        }

        return@map useCaseResult
    }
        .filter { it is R }
        .map { (it as R).value }
}