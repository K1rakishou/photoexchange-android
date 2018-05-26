package com.kirakishou.photoexchange.helper.intercom

import com.kirakishou.photoexchange.helper.intercom.event.BaseEvent
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class PhotosActivityViewModelStateEventForwarder {
    private val photosActivityViewStateSubject = PublishSubject.create<BaseEvent>().toSerialized()
    private val uploadedPhotosFragmentViewStateSubject = PublishSubject.create<BaseEvent>().toSerialized()
    private val receivedPhotosFragmentViewStateSubject = PublishSubject.create<BaseEvent>().toSerialized()
    private val galleryPhotosFragmentViewStateSubject = PublishSubject.create<BaseEvent>().toSerialized()

    fun getPhotoActivityEventsStream(): Observable<BaseEvent> {
        return photosActivityViewStateSubject
    }

    fun getUploadedPhotosFragmentEventsStream(): Observable<BaseEvent> {
        return uploadedPhotosFragmentViewStateSubject
    }

    fun getReceivedPhotosFragmentEventsStream(): Observable<BaseEvent> {
        return receivedPhotosFragmentViewStateSubject
    }

    fun getGalleryPhotosFragmentEventsStream(): Observable<BaseEvent> {
        return galleryPhotosFragmentViewStateSubject
    }

    fun sendPhotoActivityEvent(event: BaseEvent) {
        photosActivityViewStateSubject.onNext(event)
    }

    fun sendUploadedPhotosFragmentEvent(event: BaseEvent) {
        uploadedPhotosFragmentViewStateSubject.onNext(event)
    }

    fun sendReceivedPhotosFragmentEvent(event: BaseEvent) {
        receivedPhotosFragmentViewStateSubject.onNext(event)
    }

    fun sendGalleryPhotosFragmentEvent(event: BaseEvent) {
        galleryPhotosFragmentViewStateSubject.onNext(event)
    }
}