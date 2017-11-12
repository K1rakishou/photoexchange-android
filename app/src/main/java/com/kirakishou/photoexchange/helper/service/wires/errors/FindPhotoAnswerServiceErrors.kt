package com.kirakishou.photoexchange.helper.service.wires.errors

import io.reactivex.Observable

/**
 * Created by kirakishou on 11/12/2017.
 */
interface FindPhotoAnswerServiceErrors {
    fun onUnknownErrorObservable(): Observable<Throwable>
}