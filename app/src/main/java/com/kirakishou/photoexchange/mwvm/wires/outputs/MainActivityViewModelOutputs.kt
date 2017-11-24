package com.kirakishou.photoexchange.mwvm.wires.outputs

import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto
import io.reactivex.Observable

/**
 * Created by kirakishou on 11/3/2017.
 */
interface MainActivityViewModelOutputs {
    fun onTakenPhotoSavedObservable(): Observable<TakenPhoto>
}