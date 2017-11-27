package com.kirakishou.photoexchange.mwvm.wires.outputs

import com.kirakishou.photoexchange.mwvm.model.dto.PhotoAnswerAllFound
import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer
import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto
import io.reactivex.Observable

/**
 * Created by kirakishou on 11/8/2017.
 */
interface AllPhotosViewActivityViewModelOutputs {
    fun onUploadedPhotosPageReceivedObservable(): Observable<List<TakenPhoto>>
    fun onReceivedPhotosPageReceivedObservable(): Observable<List<PhotoAnswer>>
    fun onScrollToTopObservable(): Observable<Unit>
    fun onStartUploadingPhotosObservable(): Observable<List<Long>>
    fun onShowLookingForPhotoIndicatorObservable(): Observable<Unit>
    fun onShowPhotoUploadedOutputObservable(): Observable<TakenPhoto>
    fun onShowFailedToUploadPhotoObservable(): Observable<Unit>
    fun onShowPhotoReceivedObservable(): Observable<PhotoAnswerAllFound>
    fun onShowErrorWhileTryingToLookForPhotoObservable(): Observable<Unit>
    fun onShowNoPhotoOnServerObservable(): Observable<Unit>
    fun onShowUserNeedsToUploadMorePhotosObservable(): Observable<Unit>
    fun onStartLookingForPhotosObservable(): Observable<Unit>
    fun onQueuedUpPhotosLoadedObservable(): Observable<List<TakenPhoto>>
    fun onAllPhotosUploadedObservable(): Observable<Unit>
    fun onShowNoUploadedPhotosObservable(): Observable<Unit>
}