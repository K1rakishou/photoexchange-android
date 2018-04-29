package com.kirakishou.photoexchange.ui.viewstate

sealed class AllPhotosActivityViewStateEvent {
    class EndRefreshing : AllPhotosActivityViewStateEvent()
}