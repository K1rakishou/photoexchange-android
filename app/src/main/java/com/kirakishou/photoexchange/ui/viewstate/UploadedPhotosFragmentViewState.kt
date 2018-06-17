package com.kirakishou.photoexchange.ui.viewstate

import android.os.Bundle

class UploadedPhotosFragmentViewState(
    var showObtainCurrentLocationNotification: Boolean = false,
    var lastId: Long = Long.MAX_VALUE
) : BaseViewState {

    fun updateLastId(lastId: Long) {
        this.lastId = lastId
    }

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

    companion object {
        const val SHOW_OBTAIN_CURRENT_LOCATION_NOTIFICATION = "show_obtain_current_location_notification"
    }
}