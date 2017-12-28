package com.kirakishou.photoexchange.mwvm.wires.outputs

import com.kirakishou.photoexchange.mwvm.model.dto.PhotoAnswerReturnValue
import com.kirakishou.photoexchange.mwvm.model.state.FindPhotoState
import com.kirakishou.photoexchange.mwvm.viewmodel.FindPhotoAnswerServiceViewModel
import io.reactivex.Observable

/**
 * Created by kirakishou on 11/12/2017.
 */
interface FindPhotoAnswerServiceOutputs {
    fun findPhotoStateObservable(): Observable<FindPhotoState>
}