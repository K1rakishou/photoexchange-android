package com.kirakishou.photoexchange.mvp.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

class GalleryPhotoIdsResponse
private constructor(

    @Expose
    @SerializedName("gallery_photo_ids")
    val galleryPhotoIds: List<Long>,

    errorCode: ErrorCode.GalleryPhotosErrors
) : StatusResponse(errorCode.value, errorCode) {

    companion object {
        fun success(galleryPhotoIds: List<Long>): GalleryPhotoIdsResponse {
            return GalleryPhotoIdsResponse(galleryPhotoIds, ErrorCode.GalleryPhotosErrors.Remote.Ok())
        }

        fun error(errorCode: ErrorCode.GalleryPhotosErrors): GalleryPhotoIdsResponse {
            return GalleryPhotoIdsResponse(emptyList(), errorCode)
        }
    }

//    inner class GalleryPhotoResponse(
//
//        @Expose
//        @SerializedName("id")
//        val id: Long,
//
//        @Expose
//        @SerializedName("photo_name")
//        val photoName: String,
//
//        @Expose
//        @SerializedName("lon")
//        val lon: Double,
//
//        @Expose
//        @SerializedName("lat")
//        val lat: Double,
//
//        @Expose
//        @SerializedName("uploaded_on")
//        val uploadedOn: Long,
//
//        @Expose
//        @SerializedName("favourites_count")
//        val favouritesCount: Long,
//
//        @Expose
//        @SerializedName("is_favourited")
//        val isFavourited: Boolean,
//
//        @Expose
//        @SerializedName("is_reported")
//        val isReported: Boolean
//    )
}