package com.kirakishou.photoexchange.helper.api.mapper

import com.kirakishou.photoexchange.mvp.model.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.net.response.GalleryPhotosResponse

object GalleryPhotoResponseMapper {

    fun toGalleryPhoto(galleryPhotoResponseList: List<GalleryPhotosResponse.GalleryPhotoResponse>): List<GalleryPhoto> {
        return galleryPhotoResponseList.map { GalleryPhoto(0, it.photoName, it.lon, it.lat, it.uploadedOn) }
    }
}