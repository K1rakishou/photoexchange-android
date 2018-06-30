package com.kirakishou.photoexchange.mvp.model.exception

import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

sealed class GeneralException : Exception() {
    class ErrorCodeException(val errorCode: ErrorCode) : GeneralException()
    class ApiException(val errorCode: ErrorCode) : GeneralException()
}

sealed class UploadPhotoServiceException : Exception() {
    class CouldNotGetUserIdException(val errorCode: ErrorCode.GetUserIdError) : UploadPhotoServiceException()
}

sealed class ReceivePhotosServiceException : Exception() {
    class PhotoNamesAreEmpty : ReceivePhotosServiceException()
    class CouldNotGetUserId : ReceivePhotosServiceException()
    class ApiException(val remoteErrorCode: ErrorCode.ReceivePhotosErrors) : ReceivePhotosServiceException()
}

sealed class PhotoUploadingException : Exception() {
    class PhotoDoesNotExistOnDisk : PhotoUploadingException()
    class CouldNotRotatePhoto : PhotoUploadingException()
    class DatabaseException : PhotoUploadingException()
    class CouldNotUpdatePhotoState : PhotoUploadingException()
    class ApiException(val remoteErrorCode: ErrorCode.UploadPhotoErrors) : PhotoUploadingException()
}

sealed class GetReceivedPhotosException : Exception() {
    class OnKnownError(val errorCode: ErrorCode.GetReceivedPhotosErrors) : GetReceivedPhotosException()
}

sealed class GetGalleryPhotosException : Exception() {
    class OnKnownError(val errorCode: ErrorCode.GetGalleryPhotosErrors) : GetGalleryPhotosException()
}

sealed class GetGalleryPhotosInfoException : Exception() {
    class OnKnownError(val errorCode: ErrorCode.GetGalleryPhotosErrors) : GetGalleryPhotosInfoException()
}

sealed class GetUploadedPhotosException : Exception() {
    class OnKnownError(val errorCode: ErrorCode.GetUploadedPhotosErrors) : GetUploadedPhotosException()
}