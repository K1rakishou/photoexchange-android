package com.kirakishou.photoexchange.mvp.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

class GalleryPhotoInfoResponse
private constructor(

    @Expose
    @SerializedName("gallery_photos_info")
    val galleryPhotosInfo: List<GalleryPhotosInfoData>,

    errorCode: ErrorCode
) : StatusResponse(errorCode.getValue(), errorCode) {

    companion object {
        fun success(galleryPhotosInfo: List<GalleryPhotosInfoData>): GalleryPhotoInfoResponse {
            return GalleryPhotoInfoResponse(galleryPhotosInfo, ErrorCode.GalleryPhotosErrors.Ok())
        }

        fun fail(errorCode: ErrorCode): GalleryPhotoInfoResponse {
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