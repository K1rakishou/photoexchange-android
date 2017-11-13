package com.kirakishou.photoexchange.mwvm.wires.errors

import com.kirakishou.photoexchange.mwvm.model.other.ServerErrorCode
import io.reactivex.Observable

/**
 * Created by kirakishou on 11/4/2017.
 */
interface UploadPhotoServiceErrors {
    fun onBadResponseObservable(): Observable<ServerErrorCode>
    fun onUnknownErrorObservable(): Observable<Throwable>
}