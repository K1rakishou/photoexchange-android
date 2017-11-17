package com.kirakishou.photoexchange.mwvm.wires.outputs

import com.kirakishou.photoexchange.mwvm.model.dto.PhotoAnswerReturnValue
import io.reactivex.Observable

/**
 * Created by kirakishou on 11/12/2017.
 */
interface FindPhotoAnswerServiceOutputs {
    fun uploadMorePhotosObservable(): Observable<Unit>
    fun onPhotoAnswerFoundObservable(): Observable<PhotoAnswerReturnValue>
    fun userHasNoUploadedPhotosObservable(): Observable<Unit>
    fun noPhotosToSendBackObservable(): Observable<Unit>
    fun couldNotMarkPhotoAsReceivedObservable(): Observable<Unit>
}