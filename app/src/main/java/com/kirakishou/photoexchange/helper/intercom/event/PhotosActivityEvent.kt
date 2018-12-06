package com.kirakishou.photoexchange.helper.intercom.event

sealed class PhotosActivityEvent : BaseEvent {
  class StartUploadingService(val callerClass: Class<*>,
                              val reason: String) : PhotosActivityEvent()

  class StartReceivingService(val callerClass: Class<*>,
                              val reason: String) : PhotosActivityEvent()

  class CancelPhotoUploading(val photoId: Long) : PhotosActivityEvent()

  class ScrollEvent(val isScrollingDown: Boolean) : PhotosActivityEvent()

  object OnNewPhotoReceived : PhotosActivityEvent()
  class OnNewGalleryPhotos(val count: Int) : PhotosActivityEvent()

  class ShowToast(val message: String) : PhotosActivityEvent()
}