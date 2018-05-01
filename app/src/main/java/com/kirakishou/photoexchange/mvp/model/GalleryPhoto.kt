package com.kirakishou.photoexchange.mvp.model

class GalleryPhoto(
    val remoteId: Long,
    val photoName: String,
    val lon: Double,
    val lat: Double,
    val uploadedOn: Long,
    val favouritesCount: Long
)