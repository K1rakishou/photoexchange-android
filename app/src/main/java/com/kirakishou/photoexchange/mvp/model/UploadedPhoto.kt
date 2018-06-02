package com.kirakishou.photoexchange.mvp.model

data class UploadedPhoto(
    var photoId: Long,
    val photoName: String,
    val uploaderLon: Double,
    val uploaderLat: Double,
    var hasReceiverInfo: Boolean,
    val uploadedOn: Long
)