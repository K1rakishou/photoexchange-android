package com.kirakishou.photoexchange.mwvm.wires.outputs

import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto
import io.reactivex.Observable

/**
 * Created by kirakishou on 11/4/2017.
 */
interface UploadPhotoServiceOutputs {
    fun onUploadPhotoResponseObservable(): Observable<TakenPhoto>
    fun onAllPhotosUploadedObservable(): Observable<Unit>
    fun onStartUploadQueuedUpPhotosObservable(): Observable<Unit>
    fun onNoPhotosToUploadObservable(): Observable<Unit>
    fun onFailedToUploadPhotoObservable(): Observable<Long>
}