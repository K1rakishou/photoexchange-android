package com.kirakishou.photoexchange.ui.viewstate

import android.os.Bundle

class UploadedPhotosFragmentViewState(
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

    fun updateFromViewStateEvent(viewStateEvent: UploadedPhotosFragmentViewStateEvent) {
        when (viewStateEvent) {
            is UploadedPhotosFragmentViewStateEvent.ShowObtainCurrentLocationNotification -> {
                showObtainCurrentLocationNotification = true
            }
            is UploadedPhotosFragmentViewStateEvent.HideObtainCurrentLocationNotification -> {
                showObtainCurrentLocationNotification = false
            }

            is UploadedPhotosFragmentViewStateEvent.Default,
            is UploadedPhotosFragmentViewStateEvent.RemovePhoto -> {
                //Do nothing
            }
        }
    }

    companion object {
        const val SHOW_OBTAIN_CURRENT_LOCATION_NOTIFICATION = "show_obtain_current_location_notification"
    }
}