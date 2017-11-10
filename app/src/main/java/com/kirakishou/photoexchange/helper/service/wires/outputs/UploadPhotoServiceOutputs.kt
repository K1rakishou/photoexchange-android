package com.kirakishou.photoexchange.helper.service.wires.outputs

import com.kirakishou.photoexchange.mvvm.model.UploadedPhoto
import com.kirakishou.photoexchange.mvvm.model.dto.PhotoNameWithId
import io.reactivex.Observable

/**
 * Created by kirakishou on 11/4/2017.
 */
interface UploadPhotoServiceOutputs {
    fun onSendPhotoResponseObservable(): Observable<UploadedPhoto>
}