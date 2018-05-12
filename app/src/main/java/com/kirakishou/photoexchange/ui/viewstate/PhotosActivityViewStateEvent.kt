package com.kirakishou.photoexchange.ui.viewstate

sealed class PhotosActivityViewStateEvent {
    class EndRefreshing : PhotosActivityViewStateEvent()
}