package com.kirakishou.photoexchange.helper.intercom

import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.intercom.event.ReceivedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class PhotosActivityViewModelStateEventForwarder {
    private val photosActivityViewStateSubject = PublishSubject.create<PhotosActivityEvent>().toSerialized()
    private val uploadedPhotosFragmentViewStateSubject = PublishSubject.create<UploadedPhotosFragmentEvent>().toSerialized()
    private val receivedPhotosFragmentViewStateSubject = PublishSubject.create<ReceivedPhotosFragmentEvent>().toSerialized()

    fun getPhotoActivityEventsStream(): Observable<PhotosActivityEvent> {
        return photosActivityViewStateSubject
    }

    fun getUploadedPhotosFragmentEventsStream(): Observable<UploadedPhotosFragmentEvent> {
        return uploadedPhotosFragmentViewStateSubject
    }

    fun getReceivedPhotosFragmentEventsStream(): Observable<ReceivedPhotosFragmentEvent> {
        return receivedPhotosFragmentViewStateSubject
    }

    fun sendPhotoActivityEvent(event: PhotosActivityEvent) {
        photosActivityViewStateSubject.onNext(event)
    }

    fun sendUploadedPhotosFragmentEvent(event: UploadedPhotosFragmentEvent) {
        uploadedPhotosFragmentViewStateSubject.onNext(event)
    }

    fun sendReceivedPhotosFragmentEvent(event: ReceivedPhotosFragmentEvent) {
        receivedPhotosFragmentViewStateSubject.onNext(event)
    }
}