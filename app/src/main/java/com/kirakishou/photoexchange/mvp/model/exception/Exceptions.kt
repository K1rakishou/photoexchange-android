package com.kirakishou.photoexchange.mvp.model.exception

import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

sealed class GeneralException : Exception() {
    class ErrorCodeException(val errorCode: ErrorCode) : GeneralException()
    class ApiException(val errorCode: ErrorCode) : GeneralException()
}

sealed class UploadPhotoServiceException : Exception() {
    class CouldNotGetUserIdException(val errorCode: ErrorCode.GetUserIdError) : UploadPhotoServiceException()
}

sealed class ReceivePhotosServiceException : Exception() {
    class NoUploadedPhotos : ReceivePhotosServiceException()
    class CouldNotGetUserId : ReceivePhotosServiceException()
    class NoPhotosToSendBack : ReceivePhotosServiceException()
    class OnKnownError(val errorCode: ErrorCode.ReceivePhotosErrors) : ReceivePhotosServiceException()
}

sealed class PhotoUploadingException(val takenPhoto: TakenPhoto) : Exception() {
    class PhotoDoesNotExistOnDisk(takenPhoto: TakenPhoto) : PhotoUploadingException(takenPhoto)
    class CouldNotRotatePhoto(takenPhoto: TakenPhoto) : PhotoUploadingException(takenPhoto)
    class DatabaseException(takenPhoto: TakenPhoto) : PhotoUploadingException(takenPhoto)
    class RemoteServerException(val remoteErrorCode: ErrorCode.UploadPhotoErrors,
                                takenPhoto: TakenPhoto) : PhotoUploadingException(takenPhoto)
}

sealed class GetReceivedPhotosException : Exception() {
    class OnKnownError(val errorCode: ErrorCode.GetReceivedPhotosErrors) : GetReceivedPhotosException()
}