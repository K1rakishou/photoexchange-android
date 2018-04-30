package com.kirakishou.photoexchange.helper.extension

import io.reactivex.Observable
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