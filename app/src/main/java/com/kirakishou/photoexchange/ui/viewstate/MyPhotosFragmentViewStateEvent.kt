package com.kirakishou.photoexchange.ui.viewstate

import com.kirakishou.photoexchange.mvp.model.MyPhoto

sealed class MyPhotosFragmentViewStateEvent {
    class Default : MyPhotosFragmentViewStateEvent()
    class ShowObtainCurrentLocationNotification : MyPhotosFragmentViewStateEvent()
    class HideObtainCurrentLocationNotification : MyPhotosFragmentViewStateEvent()
    class RemovePhoto(val photo: MyPhoto) : MyPhotosFragmentViewStateEvent()
    class AddPhoto(val photo: MyPhoto) : MyPhotosFragmentViewStateEvent()
    class ScrollToTop : MyPhotosFragmentViewStateEvent()
}