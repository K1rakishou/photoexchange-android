package com.kirakishou.photoexchange.helper.service.wires.outputs

import com.kirakishou.photoexchange.mvvm.model.PhotoAnswer
import io.reactivex.Observable

/**
 * Created by kirakishou on 11/12/2017.
 */
interface FindPhotoAnswerServiceOutputs {
    fun onPhotoAnswerFoundObservable(): Observable<PhotoAnswer>
}