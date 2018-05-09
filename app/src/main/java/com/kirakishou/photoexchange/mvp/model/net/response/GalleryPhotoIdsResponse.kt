package com.kirakishou.photoexchange.mvp.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

class GalleryPhotoIdsResponse
private constructor(

    @Expose
    @SerializedName("gallery_photo_ids")
    val galleryPhotoIds: List<Long>,

    errorCode: ErrorCode.GetGalleryPhotosErrors
) : StatusResponse(errorCode.value, errorCode) {

    companion object {
        fun success(galleryPhotoIds: List<Long>): GalleryPhotoIdsResponse {
            return GalleryPhotoIdsResponse(galleryPhotoIds, ErrorCode.GetGalleryPhotosErrors.Remote.Ok())
        }

        fun error(errorCode: ErrorCode.GetGalleryPhotosErrors): GalleryPhotoIdsResponse {
            return GalleryPhotoIdsResponse(emptyList(), errorCode)
        }
    }
}