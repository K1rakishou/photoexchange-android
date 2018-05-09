package com.kirakishou.photoexchange.mvp.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

class GalleryPhotoInfoResponse
private constructor(

    @Expose
    @SerializedName("gallery_photos_info")
    val galleryPhotosInfo: List<GalleryPhotosInfoData>,

    errorCode: ErrorCode.GetGalleryPhotosInfoError
) : StatusResponse(errorCode.value, errorCode) {

    companion object {
        fun success(galleryPhotosInfo: List<GalleryPhotosInfoData>): GalleryPhotoInfoResponse {
            return GalleryPhotoInfoResponse(galleryPhotosInfo, ErrorCode.GetGalleryPhotosInfoError.Remote.Ok())
        }

        fun fail(errorCode: ErrorCode.GetGalleryPhotosInfoError): GalleryPhotoInfoResponse {
            return GalleryPhotoInfoResponse(emptyList(), errorCode)
        }
    }

    class GalleryPhotosInfoData(

        @Expose
        @SerializedName("id")
        val id: Long,

        @Expose
        @SerializedName("is_favourited")
        val isFavourited: Boolean,

        @Expose
        @SerializedName("is_reported")
        val isReported: Boolean
    )
}