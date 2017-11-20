package com.kirakishou.photoexchange.mwvm.wires.errors

import io.reactivex.Observable

/**
 * Created by kirakishou on 11/3/2017.
 */
interface MainActivityViewModelErrors {
    fun onUnknownErrorObservable(): Observable<Throwable>
}