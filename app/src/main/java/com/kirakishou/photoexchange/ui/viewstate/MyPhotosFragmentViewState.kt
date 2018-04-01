package com.kirakishou.photoexchange.ui.viewstate

sealed class MyPhotosFragmentViewState {
    class Default : MyPhotosFragmentViewState()
    class ShowObtainCurrentLocationNotification(val show: Boolean) : MyPhotosFragmentViewState()
}