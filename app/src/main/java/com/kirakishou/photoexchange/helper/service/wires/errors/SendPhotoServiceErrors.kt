package com.kirakishou.photoexchange.helper.service.wires.errors

import com.kirakishou.photoexchange.mvvm.model.ServerErrorCode
import io.reactivex.Observable

/**
 * Created by kirakishou on 11/4/2017.
 */
interface SendPhotoServiceErrors {
    fun onBadResponseObservable(): Observable<ServerErrorCode>
    fun onUnknownErrorObservable(): Observable<Throwable>
}