package com.kirakishou.photoexchange.ui.viewstate

import com.kirakishou.photoexchange.mvp.model.TakenPhoto

sealed class UploadedPhotosFragmentViewStateEvent {
    class Default : UploadedPhotosFragmentViewStateEvent()
    class ShowObtainCurrentLocationNotification : UploadedPhotosFragmentViewStateEvent()
    class HideObtainCurrentLocationNotification : UploadedPhotosFragmentViewStateEvent()
    class ShowProgressFooter : UploadedPhotosFragmentViewStateEvent()
    class HideProgressFooter : UploadedPhotosFragmentViewStateEvent()
    class RemovePhoto(val photo: TakenPhoto) : UploadedPhotosFragmentViewStateEvent()
    class AddPhoto(val photo: TakenPhoto) : UploadedPhotosFragmentViewStateEvent()
    class ScrollToTop : UploadedPhotosFragmentViewStateEvent()
}