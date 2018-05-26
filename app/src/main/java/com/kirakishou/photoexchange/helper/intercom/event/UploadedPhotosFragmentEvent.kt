package com.kirakishou.photoexchange.helper.intercom.event

import com.kirakishou.photoexchange.mvp.model.TakenPhoto

sealed class UploadedPhotosFragmentEvent : BaseEvent {
    class ShowObtainCurrentLocationNotification : UploadedPhotosFragmentEvent()
    class HideObtainCurrentLocationNotification : UploadedPhotosFragmentEvent()
    class ShowProgressFooter : UploadedPhotosFragmentEvent()
    class HideProgressFooter : UploadedPhotosFragmentEvent()
    class RemovePhoto(val photo: TakenPhoto) : UploadedPhotosFragmentEvent()
    class AddPhoto(val photo: TakenPhoto) : UploadedPhotosFragmentEvent()
    class ScrollToTop : UploadedPhotosFragmentEvent()
}