package com.kirakishou.photoexchange.ui.adapter

/**
 * Created by kirakishou on 11/7/2017.
 */
enum class AdapterItemType(val type: Int) {
    EMPTY(0),
    VIEW_MY_PHOTO(1),
    VIEW_PROGRESS(2),
    VIEW_QUEUED_UP_PHOTOS_NOTIFICATION(3),
    VIEW_OBTAIN_CURRENT_LOCATION_NOTIFICATION(4),
    VIEW_FAILED_TO_UPLOAD_PHOTOS_NOTIFICATION(5);

    companion object {
        fun fromInt(type: Int): AdapterItemType {
            return AdapterItemType.values().first { it.ordinal == type }
        }
    }
}