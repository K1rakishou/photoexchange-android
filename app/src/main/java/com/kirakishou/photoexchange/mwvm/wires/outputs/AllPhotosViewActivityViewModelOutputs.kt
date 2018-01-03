package com.kirakishou.photoexchange.mwvm.wires.outputs

import com.kirakishou.photoexchange.mwvm.model.other.MulticastEvent
import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer
import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto
import com.kirakishou.photoexchange.mwvm.model.state.LookingForPhotoState
import com.kirakishou.photoexchange.mwvm.model.state.PhotoUploadingState
import io.reactivex.Observable

/**
 * Created by kirakishou on 11/8/2017.
 */
interface AllPhotosViewActivityViewModelOutputs {
    fun onUploadedPhotosPageReceivedObservable(): Observable<List<TakenPhoto>>
    fun onReceivedPhotosPageReceivedObservable(): Observable<List<PhotoAnswer>>
    fun onScrollToTopObservable(): Observable<Unit>
    fun onShowLookingForPhotoIndicatorObservable(): Observable<Unit>
    fun onStartLookingForPhotosObservable(): Observable<Unit>
    fun onStartPhotosUploadingObservable(): Observable<Unit>
    fun onQueuedUpAndFailedToUploadLoadedObservable(): Observable<List<TakenPhoto>>
    fun onTakenPhotoUploadingCanceledObservable(): Observable<Long>
    fun onBeginReceivingEventsObservable(): Observable<Class<*>>
    fun onStopReceivingEventsObservable(): Observable<Class<*>>

    fun onPhotoUploadingStateObservable(): Observable<MulticastEvent<PhotoUploadingState>>
    fun onLookingForPhotoStateObservable(): Observable<MulticastEvent<LookingForPhotoState>>
}