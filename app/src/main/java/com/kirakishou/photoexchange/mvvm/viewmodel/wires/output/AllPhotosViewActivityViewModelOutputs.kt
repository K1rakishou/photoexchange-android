package com.kirakishou.photoexchange.mvvm.viewmodel.wires.output

import com.kirakishou.photoexchange.mvvm.model.TakenPhoto
import io.reactivex.Observable

/**
 * Created by kirakishou on 11/8/2017.
 */
interface AllPhotosViewActivityViewModelOutputs {
    fun onTakenPhotosPageFetchedObservable(): Observable<List<TakenPhoto>>
    fun onLastTakenPhotoObservable(): Observable<TakenPhoto>
}