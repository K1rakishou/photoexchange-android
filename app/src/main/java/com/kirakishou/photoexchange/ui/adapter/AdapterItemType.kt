package com.kirakishou.photoexchange.ui.adapter

/**
 * Created by kirakishou on 11/7/2017.
 */
enum class AdapterItemType(val type: Int) {
    EMPTY(0),
    VIEW_TAKEN_PHOTO(1),
    VIEW_UPLOADED_PHOTO(2),
    VIEW_PROGRESS(3),
    VIEW_OBTAIN_CURRENT_LOCATION_NOTIFICATION(4),
    VIEW_FAILED_TO_UPLOAD(5),
    VIEW_RECEIVED_PHOTO(6),
    VIEW_GALLERY_PHOTO(7);

    companion object {
        fun fromInt(type: Int): AdapterItemType {
            return AdapterItemType.values().first { it.ordinal == type }
        }
    }
}