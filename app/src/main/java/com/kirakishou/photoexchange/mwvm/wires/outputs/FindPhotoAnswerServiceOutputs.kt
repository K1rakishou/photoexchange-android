package com.kirakishou.photoexchange.mwvm.wires.outputs

import com.kirakishou.photoexchange.mwvm.model.state.LookingForPhotoState
import io.reactivex.Observable

/**
 * Created by kirakishou on 11/12/2017.
 */
interface FindPhotoAnswerServiceOutputs {
    fun findPhotoStateObservable(): Observable<LookingForPhotoState>
}