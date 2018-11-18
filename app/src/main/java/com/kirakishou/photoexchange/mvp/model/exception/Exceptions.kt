package com.kirakishou.photoexchange.mvp.model.exception

import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

class EmptyUserIdException : Exception()
class ApiErrorException(val errorCode: ErrorCode) : Exception()
class BadServerResponse : Exception()
class UnknownException(val exception: Exception) : Exception()
class DatabaseException(message: String) : Exception(message)

//sealed class GeneralException : Exception() {
//  class ErrorCodeException(val errorCode: ErrorCode) : GeneralException()
//}
//
//sealed class UploadPhotoServiceException : Exception() {
//  class CouldNotGetUserIdException(val errorCode: ErrorCode) : UploadPhotoServiceException()
//}
//
//sealed class ReceivePhotosServiceException : Exception() {
//  class PhotoNamesAreEmpty : ReceivePhotosServiceException()
//  class CouldNotGetUserId : ReceivePhotosServiceException()
//  class ApiException(val remoteErrorCode: ErrorCode) : ReceivePhotosServiceException()
//  class NoUploadedPhotosWithoutReceiverInfo() : ReceivePhotosServiceException()
//}
//
//sealed class PhotoUploadingException : Exception() {
//  class PhotoDoesNotExistOnDisk : PhotoUploadingException()
//  class CouldNotRotatePhoto : PhotoUploadingException()
//  class DatabaseException : PhotoUploadingException()
//  class CouldNotUpdatePhotoState : PhotoUploadingException()
//  class ApiException(val remoteErrorCode: ErrorCode) : PhotoUploadingException()
//}