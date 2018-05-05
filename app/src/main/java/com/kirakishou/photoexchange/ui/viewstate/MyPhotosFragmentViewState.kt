package com.kirakishou.photoexchange.ui.viewstate

import android.os.Bundle

class MyPhotosFragmentViewState(
    var showObtainCurrentLocationNotification: Boolean = false
) : BaseViewState {
    override fun saveToBundle(bundle: Bundle?) {
        if (bundle == null) {
            return
        }

        bundle.putBoolean(SHOW_OBTAIN_CURRENT_LOCATION_NOTIFICATION, showObtainCurrentLocationNotification)
    }

    override fun loadFromBundle(bundle: Bundle?) {
        if (bundle != null) {
            showObtainCurrentLocationNotification = bundle.getBoolean(SHOW_OBTAIN_CURRENT_LOCATION_NOTIFICATION, false)
        }
    }

    fun updateFromViewStateEvent(viewStateEvent: MyPhotosFragmentViewStateEvent) {
        when (viewStateEvent) {
            is MyPhotosFragmentViewStateEvent.ShowObtainCurrentLocationNotification -> {
                showObtainCurrentLocationNotification = true
            }
            is MyPhotosFragmentViewStateEvent.HideObtainCurrentLocationNotification -> {
                showObtainCurrentLocationNotification = false
            }

            is MyPhotosFragmentViewStateEvent.Default,
            is MyPhotosFragmentViewStateEvent.RemovePhoto -> {
                //Do nothing
            }
        }
    }

    companion object {
        const val SHOW_OBTAIN_CURRENT_LOCATION_NOTIFICATION = "show_obtain_current_location_notification"
    }
}