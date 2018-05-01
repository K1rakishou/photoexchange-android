package com.kirakishou.photoexchange.mvp.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

class GalleryPhotosResponse
private constructor(

    @Expose
    @SerializedName("gallery_photos")
    val galleryPhotos: List<GalleryPhotoResponse>,

    errorCode: ErrorCode.GalleryPhotosErrors
) : StatusResponse(errorCode.value, errorCode) {

    companion object {
        fun success(galleryPhotos: List<GalleryPhotoResponse>): GalleryPhotosResponse {
            return GalleryPhotosResponse(galleryPhotos, ErrorCode.GalleryPhotosErrors.Remote.Ok())
        }

        fun error(errorCode: ErrorCode.GalleryPhotosErrors): GalleryPhotosResponse {
            return GalleryPhotosResponse(emptyList(), errorCode)
        }
    }

    inner class GalleryPhotoResponse(

        @Expose
        @SerializedName("id")
        val id: Long,

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
        val uploadedOn: Long,

        @Expose
        @SerializedName("likes_count")
        val likesCount: Long
    )
}