package com.kirakishou.photoexchange.mwvm.model.state

import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer

/**
 * Created by kirakishou on 12/28/2017.
 */
sealed class FindPhotoState {
    class UploadMorePhotos : FindPhotoState()
    class LocalRepositoryError : FindPhotoState()
    class ServerHasNoPhotos : FindPhotoState()
    class PhotoFound(val photoAnswer: PhotoAnswer, val allFound: Boolean) : FindPhotoState()
    class UnknownError(val error: Throwable) : FindPhotoState()
}