package com.kirakishou.photoexchange.mwvm.wires.errors

import io.reactivex.Observable

/**
 * Created by kirakishou on 11/8/2017.
 */
interface AllPhotosViewActivityViewModelErrors {
    fun onUnknownErrorObservable(): Observable<Throwable>
}