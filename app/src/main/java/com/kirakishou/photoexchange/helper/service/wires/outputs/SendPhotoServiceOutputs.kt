package com.kirakishou.photoexchange.helper.service.wires.outputs

import com.kirakishou.photoexchange.mvvm.model.ServerErrorCode
import com.kirakishou.photoexchange.mvvm.model.net.response.StatusResponse
import io.reactivex.Observable

/**
 * Created by kirakishou on 11/4/2017.
 */
interface SendPhotoServiceOutputs {
    fun onSendPhotoResponseObservable(): Observable<ServerErrorCode>
}