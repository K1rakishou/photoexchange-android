package com.kirakishou.photoexchange.mwvm.wires.outputs

import com.kirakishou.photoexchange.mwvm.model.dto.PhotoAnswerAllFound
import com.kirakishou.photoexchange.mwvm.model.other.MulticastEvent
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
    fun onStartUploadingPhotosObservable(): Observable<MulticastEvent<Unit>>
    fun onShowLookingForPhotoIndicatorObservable(): Observable<Unit>
    fun onShowPhotoUploadedOutputObservable(): Observable<MulticastEvent<TakenPhoto>>
    fun onShowFailedToUploadPhotoObservable(): Observable<MulticastEvent<TakenPhoto>>
    fun onShowPhotoReceivedObservable(): Observable<PhotoAnswerAllFound>
    fun onShowErrorWhileTryingToLookForPhotoObservable(): Observable<Unit>
    fun onShowNoPhotoOnServerObservable(): Observable<Unit>
    fun onShowUserNeedsToUploadMorePhotosObservable(): Observable<Unit>
    fun onStartLookingForPhotosObservable(): Observable<Unit>
    fun onQueuedUpAndFailedToUploadLoadedObservable(): Observable<List<TakenPhoto>>
    fun onAllPhotosUploadedObservable(): Observable<MulticastEvent<Unit>>
    fun onShowNoUploadedPhotosObservable(): Observable<Unit>
    fun onTakenPhotoUploadingCanceledObservable(): Observable<Long>
    fun onBeginReceivingEventsObservable(): Observable<Class<*>>
    fun onStopReceivingEventsObservable(): Observable<Class<*>>
    fun onPhotoMarkedToBeUploadedObservable(): Observable<Unit>
}