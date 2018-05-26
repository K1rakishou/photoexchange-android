package com.kirakishou.photoexchange.ui.viewstate

sealed class PhotosActivityEvent {
    class EndRefreshing : PhotosActivityEvent()
}