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
        bundle.putLong(LAST_ID, lastId)
    }

    override fun loadFromBundle(bundle: Bundle?) {
        if (bundle != null) {
            showObtainCurrentLocationNotification = bundle.getBoolean(SHOW_OBTAIN_CURRENT_LOCATION_NOTIFICATION, false)
            lastId = bundle.getLong(LAST_ID, Long.MAX_VALUE)
        }
    }

    companion object {
        const val SHOW_OBTAIN_CURRENT_LOCATION_NOTIFICATION = "show_obtain_current_location_notification"
        const val LAST_ID = "last_id"
    }
}