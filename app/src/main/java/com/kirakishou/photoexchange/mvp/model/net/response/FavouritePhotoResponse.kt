package com.kirakishou.photoexchange.mvp.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

class FavouritePhotoResponse
private constructor(

    @Expose
    @SerializedName("is_favourited")
    val isFavourited: Boolean,

    @Expose
    @SerializedName("count")
    val favouritesCount: Long,

    errorCode: ErrorCode.FavouritePhotoErrors
) : StatusResponse(errorCode.value, errorCode) {

    companion object {
        fun success(isFavourited: Boolean, count: Long): FavouritePhotoResponse {
            return FavouritePhotoResponse(isFavourited, count, ErrorCode.FavouritePhotoErrors.Remote.Ok())
        }

        fun error(errorCode: ErrorCode.FavouritePhotoErrors): FavouritePhotoResponse {
            return FavouritePhotoResponse(false, 0, errorCode)
        }
    }
}