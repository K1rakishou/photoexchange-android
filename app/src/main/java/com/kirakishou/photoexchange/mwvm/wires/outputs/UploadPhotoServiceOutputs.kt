package com.kirakishou.photoexchange.mwvm.wires.outputs

import com.kirakishou.photoexchange.mwvm.model.state.PhotoUploadingState
import io.reactivex.Observable

/**
 * Created by kirakishou on 11/4/2017.
 */
interface UploadPhotoServiceOutputs {
    fun onPhotoUploadStateObservable(): Observable<PhotoUploadingState>
}