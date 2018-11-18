package com.kirakishou.photoexchange.mvp.model.exception

import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

class ApiException(val errorCode: ErrorCode) : Exception()
class EmptyUserIdException : Exception()

sealed class GeneralException : Exception() {
  class ErrorCodeException(val errorCode: ErrorCode) : GeneralException()
}

sealed class UploadPhotoServiceException : Exception() {
  class CouldNotGetUserIdException(val errorCode: ErrorCode) : UploadPhotoServiceException()
}

sealed class ReceivePhotosServiceException : Exception() {
  class PhotoNamesAreEmpty : ReceivePhotosServiceException()
  class CouldNotGetUserId : ReceivePhotosServiceException()
  class ApiException(val remoteErrorCode: ErrorCode) : ReceivePhotosServiceException()
  class NoUploadedPhotosWithoutReceiverInfo() : ReceivePhotosServiceException()
}

sealed class PhotoUploadingException : Exception() {
  class PhotoDoesNotExistOnDisk : PhotoUploadingException()
  class CouldNotRotatePhoto : PhotoUploadingException()
  class DatabaseException : PhotoUploadingException()
  class CouldNotUpdatePhotoState : PhotoUploadingException()
  class ApiException(val remoteErrorCode: ErrorCode) : PhotoUploadingException()
}

sealed class GetReceivedPhotosException : Exception() {
  class OnKnownError(val errorCode: ErrorCode) : GetReceivedPhotosException()
}

sealed class GetGalleryPhotosException : Exception() {
  class OnKnownError(val errorCode: ErrorCode) : GetGalleryPhotosException()
}

sealed class GetGalleryPhotosInfoException : Exception() {
  class OnKnownError(val errorCode: ErrorCode) : GetGalleryPhotosInfoException()
}

sealed class GetUploadedPhotosException : Exception() {
  class OnKnownError(val errorCode: ErrorCode) : GetUploadedPhotosException()
}

sealed class ReportPhotoExceptions : Exception() {
  class ApiErrorException(val errorCode: ErrorCode) : ReportPhotoExceptions()
  class BadServerResponse : ReportPhotoExceptions()
  class UnknownException(val exception: Exception) : ReportPhotoExceptions()
}