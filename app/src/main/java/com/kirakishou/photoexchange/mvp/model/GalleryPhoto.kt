package com.kirakishou.photoexchange.mvp.model

class GalleryPhoto(
    val galleryPhotoId: Long,
    val photoName: String,
    val lon: Double,
    val lat: Double,
    val uploadedOn: Long,
    var favouritesCount: Long,
    var isFavourited: Boolean,
    var isReported: Boolean
)