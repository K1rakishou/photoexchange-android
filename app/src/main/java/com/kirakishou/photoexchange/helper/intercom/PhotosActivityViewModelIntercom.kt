package com.kirakishou.photoexchange.helper.intercom

import com.kirakishou.photoexchange.helper.intercom.event.*
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.fragment.GalleryFragment
import com.kirakishou.photoexchange.ui.fragment.ReceivedPhotosFragment
import com.kirakishou.photoexchange.ui.fragment.UploadedPhotosFragment
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class PhotosActivityViewModelIntercom {

  val photosActivityEvents = PhotosActivityEvents()
  val uploadedPhotosFragmentEvents = UploadedPhotosFragmentEvents()
  val receivedPhotosFragmentEvents = ReceivedPhotosFragmentEvents()
  val galleryFragmentEvents = GalleryFragmentEvents()

  inline fun <reified T : IntercomListener> tell(): AbstractIntercom<BaseEvent> {
    return when (T::class.java) {
      PhotosActivity::class.java -> photosActivityEvents as AbstractIntercom<BaseEvent>
      UploadedPhotosFragment::class.java -> uploadedPhotosFragmentEvents as AbstractIntercom<BaseEvent>
      ReceivedPhotosFragment::class.java -> receivedPhotosFragmentEvents as AbstractIntercom<BaseEvent>
      GalleryFragment::class.java -> galleryFragmentEvents as AbstractIntercom<BaseEvent>
      else -> throw IllegalArgumentException("Unknown type class ${T::class.java}")
    }
  }

  class PhotosActivityEvents : AbstractIntercom<PhotosActivityEvent> {
    private val photosActivityViewStateSubject = PublishSubject.create<PhotosActivityEvent>().toSerialized()

    override fun listen(): Observable<PhotosActivityEvent> {
      return photosActivityViewStateSubject
    }

    override fun that(event: PhotosActivityEvent) {
      to(event)
    }

    override fun to(event: PhotosActivityEvent) {
      photosActivityViewStateSubject.onNext(event)
    }
  }

  class UploadedPhotosFragmentEvents : AbstractIntercom<UploadedPhotosFragmentEvent> {
    private val uploadedPhotosFragmentViewStateSubject = PublishSubject.create<UploadedPhotosFragmentEvent>().toSerialized()

    override fun listen(): Observable<UploadedPhotosFragmentEvent> {
      return uploadedPhotosFragmentViewStateSubject
    }

    override fun that(event: UploadedPhotosFragmentEvent) {
      to(event)
    }

    override fun to(event: UploadedPhotosFragmentEvent) {
      uploadedPhotosFragmentViewStateSubject.onNext(event)
    }
  }

  class ReceivedPhotosFragmentEvents : AbstractIntercom<ReceivedPhotosFragmentEvent> {
    private val receivedPhotosFragmentViewStateSubject = PublishSubject.create<ReceivedPhotosFragmentEvent>().toSerialized()

    override fun listen(): Observable<ReceivedPhotosFragmentEvent> {
      return receivedPhotosFragmentViewStateSubject
    }

    override fun that(event: ReceivedPhotosFragmentEvent) {
      to(event)
    }

    override fun to(event: ReceivedPhotosFragmentEvent) {
      receivedPhotosFragmentViewStateSubject.onNext(event)
    }
  }

  class GalleryFragmentEvents : AbstractIntercom<GalleryFragmentEvent> {
    private val galleryFragmentViewStateSubject = PublishSubject.create<GalleryFragmentEvent>().toSerialized()

    override fun listen(): Observable<GalleryFragmentEvent> {
      return galleryFragmentViewStateSubject
    }

    override fun that(event: GalleryFragmentEvent) {
      to(event)
    }

    override fun to(event: GalleryFragmentEvent) {
      galleryFragmentViewStateSubject.onNext(event)
    }
  }
}