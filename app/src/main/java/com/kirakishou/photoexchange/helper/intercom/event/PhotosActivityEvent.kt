package com.kirakishou.photoexchange.helper.intercom.event

sealed class PhotosActivityEvent : BaseEvent {
  class StartUploadingService(val callerClass: Class<*>,
                              val reason: String) : PhotosActivityEvent()

  class StartReceivingService(val callerClass: Class<*>,
                              val reason: String) : PhotosActivityEvent()

  class CancelPhotoUploading(val photoId: Long) : PhotosActivityEvent()

  class ScrollEvent(val isScrollingDown: Boolean) : PhotosActivityEvent()

  class OnNewGalleryPhotos(val count: Int) : PhotosActivityEvent()
  class OnNewReceivedPhotos(val count: Int) : PhotosActivityEvent()
  class OnNewUploadedPhotos(val count: Int) : PhotosActivityEvent()
  class ShowToast(val message: String) : PhotosActivityEvent()
  class ShowDeletePhotoDialog(val photoName: String) : PhotosActivityEvent()
}