package com.kirakishou.photoexchange.mwvm.model.event

/**
 * Created by kirakishou on 11/17/2017.
 */
enum class PhotoReceivedEventStatus {
    SUCCESS_ALL_RECEIVED,
    SUCCESS_NOT_ALL_RECEIVED,
    FAIL,
    NO_PHOTOS_ON_SERVER,
    UPLOAD_MORE_PHOTOS
}