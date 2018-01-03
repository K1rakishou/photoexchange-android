package com.kirakishou.photoexchange.mwvm.model.state

import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer

/**
 * Created by kirakishou on 12/28/2017.
 */
sealed class LookingForPhotoState {
    class UploadMorePhotos : LookingForPhotoState()
    class LocalRepositoryError : LookingForPhotoState()
    class ServerHasNoPhotos : LookingForPhotoState()
    class PhotoFound(val photoAnswer: PhotoAnswer, val allFound: Boolean) : LookingForPhotoState()
    class UnknownError(val error: Throwable) : LookingForPhotoState()
}