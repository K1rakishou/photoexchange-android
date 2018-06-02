package com.kirakishou.photoexchange.mvp.model.exception

import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

class ErrorCodeException(val errorCode: ErrorCode) : Exception()

class ApiException(val errorCode: ErrorCode) : Exception()
class CouldNotGetUserIdException(val errorCode: ErrorCode.GetUserIdError) : Exception()

sealed class PhotoUploadingException(val takenPhoto: TakenPhoto) : Exception() {
    class PhotoDoesNotExistOnDisk(takenPhoto: TakenPhoto) : PhotoUploadingException(takenPhoto)
    class CouldNotRotatePhoto(takenPhoto: TakenPhoto) : PhotoUploadingException(takenPhoto)
    class DatabaseException(takenPhoto: TakenPhoto) : PhotoUploadingException(takenPhoto)
    class RemoteServerException(val remoteErrorCode: ErrorCode.UploadPhotoErrors,
                                takenPhoto: TakenPhoto) : PhotoUploadingException(takenPhoto)
}