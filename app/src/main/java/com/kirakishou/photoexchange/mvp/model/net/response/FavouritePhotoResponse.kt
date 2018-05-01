package com.kirakishou.photoexchange.mvp.model.net.response

import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

class FavouritePhotoResponse
private constructor(
    errorCode: ErrorCode.FavouritePhotoErrors
) : StatusResponse(errorCode.value, errorCode) {

    companion object {
        fun success(): FavouritePhotoResponse {
            return FavouritePhotoResponse(ErrorCode.FavouritePhotoErrors.Remote.Ok())
        }

        fun error(errorCode: ErrorCode.FavouritePhotoErrors): FavouritePhotoResponse {
            return FavouritePhotoResponse(errorCode)
        }
    }
}