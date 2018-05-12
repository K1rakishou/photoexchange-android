package com.kirakishou.photoexchange.mvp.model

data class ReceivedPhoto(
    val id: Long?,
    val uploadedPhotoName: String,
    var photoAnswerName: String,
    var lon: Double,
    var lat: Double
)