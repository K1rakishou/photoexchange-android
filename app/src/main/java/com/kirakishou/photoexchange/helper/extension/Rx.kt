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