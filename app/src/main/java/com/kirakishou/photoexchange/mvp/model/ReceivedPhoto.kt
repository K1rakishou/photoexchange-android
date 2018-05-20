package com.kirakishou.photoexchange.mvp.model

data class ReceivedPhoto(
    val photoId: Long,
    val uploadedPhotoName: String,
    var receivedPhotoName: String,
    var lon: Double,
    var lat: Double
)