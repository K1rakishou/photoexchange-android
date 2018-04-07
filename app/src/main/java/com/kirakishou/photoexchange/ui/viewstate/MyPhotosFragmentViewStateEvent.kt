package com.kirakishou.photoexchange.ui.viewstate

sealed class MyPhotosFragmentViewStateEvent {
    class Default : MyPhotosFragmentViewStateEvent()
    class ShowObtainCurrentLocationNotification : MyPhotosFragmentViewStateEvent()
    class HideObtainCurrentLocationNotification : MyPhotosFragmentViewStateEvent()
    class RemovePhotoById(val photoId: Long) : MyPhotosFragmentViewStateEvent()
}