package com.kirakishou.photoexchange.mwvm.wires.outputs

import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer
import com.kirakishou.photoexchange.mwvm.model.other.UploadedPhoto
import io.reactivex.Observable

/**
 * Created by kirakishou on 11/8/2017.
 */
interface AllPhotosViewActivityViewModelOutputs {
    fun onUploadedPhotosPageReceivedObservable(): Observable<List<UploadedPhoto>>
    fun onReceivedPhotosPageReceivedObservable(): Observable<List<PhotoAnswer>>
}