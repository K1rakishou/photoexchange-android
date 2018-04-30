package com.kirakishou.photoexchange.mvp.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

class GalleryPhotosResponse
private constructor(

    @SerializedName("gallery_photos")
    val galleryPhotos: List<GalleryPhotoAnswer>,

    errorCode: ErrorCode.GalleryPhotosErrors
) : StatusResponse(errorCode.value, errorCode) {

    companion object {
        fun success(galleryPhotos: List<GalleryPhotoAnswer>): GalleryPhotosResponse {
            return GalleryPhotosResponse(galleryPhotos, ErrorCode.GalleryPhotosErrors.Remote.Ok())
        }

        fun error(errorCode: ErrorCode.GalleryPhotosErrors): GalleryPhotosResponse {
            return GalleryPhotosResponse(emptyList(), errorCode)
        }
    }

    inner class GalleryPhotoAnswer(
        @Expose
        @SerializedName("photo_name")
        val photoName: String,

        @Expose
        @SerializedName("lon")
        val lon: Double,

        @Expose
        @SerializedName("lat")
        val lat: Double,

        @Expose
        @SerializedName("uploaded_on")
        val uploadedOn: Long
    )
}