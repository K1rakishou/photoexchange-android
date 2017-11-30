package com.kirakishou.photoexchange.mwvm.model.other

/**
 * Created by kirakishou on 11/30/2017.
 */
enum class PhotoState(val state: String) {
    QUEUED_UP("queued_up"),
    UPLOADING("uploading"),
    FAILED_TO_UPLOAD("failed_to_upload"),
    UPLOADED("uploaded")
}