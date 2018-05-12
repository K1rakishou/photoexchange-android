package com.kirakishou.photoexchange.mvp.model.other

import com.kirakishou.photoexchange.mvp.model.TakenPhoto

/**
 * Created by kirakishou on 7/26/2017.
 */

sealed class ErrorCode(val value: Int) {

    fun toInt(): Int {
        return this.value
    }

    sealed class UploadPhotoErrors(value: Int) : ErrorCode(value) {
        sealed class Remote(value: Int) : UploadPhotoErrors(value) {
            class UnknownError : Remote(-1)
            class Ok : Remote(0)
            class BadRequest : Remote(1)
            class DatabaseError : Remote(2)
            class CouldNotGetUserId : Remote(3)
        }

        sealed class Local(value: Int) : UploadPhotoErrors(value) {
            class BadServerResponse(val message: String? = null) : Local(-1000)
            class NoPhotoFileOnDisk : Local(-1001)
            class Timeout : Local(-1002)
            class Interrupted : Local(-1003)
            class DatabaseError : Local(-1004)
        }
    }

    sealed class ReceivePhotosErrors(value: Int) : ErrorCode(value) {
        sealed class Remote(value: Int) : ReceivePhotosErrors(value) {
            class UnknownError : Remote(-1)
            class Ok : Remote(0)
            class BadRequest : Remote(1)
            class DatabaseError : Remote(2)
            class NoPhotosInRequest : Remote(3)
            class TooManyPhotosRequested : Remote(4)
            class NoPhotosToSendBack : Remote(5)
            class NotEnoughPhotosUploaded : Remote(6)
        }

        sealed class Local(value: Int) : ReceivePhotosErrors(value) {
            class BadServerResponse(val message: String? = null) : Local(-1000)
            class Timeout : Local(-1001)
        }
    }

    sealed class GetGalleryPhotosErrors(value: Int) : ErrorCode(value) {
        sealed class Remote(value: Int) : GetGalleryPhotosErrors(value) {
            class UnknownError : Remote(-1)
            class Ok : Remote(0)
            class BadRequest : Remote(1)
            class NoPhotosInRequest : Remote(2)
        }

        sealed class Local(value: Int) : GetGalleryPhotosErrors(value) {
            class BadServerResponse(val message: String? = null) : Local(-1000)
            class Timeout : Local(-1001)
            class DatabaseError : Local(-1002)
        }
    }

    sealed class GetGalleryPhotosInfoError(value: Int) : ErrorCode(value) {
        sealed class Remote(value: Int) : GetGalleryPhotosInfoError(value) {
            class UnknownError : Remote(-1)
            class Ok : Remote(0)
            class BadRequest : Remote(1)
            class NoPhotosInRequest : Remote(2)
        }

        sealed class Local(value: Int) : GetGalleryPhotosInfoError(value) {
            class BadServerResponse(val message: String? = null) : Local(-1000)
            class Timeout : Local(-1001)
            class DatabaseError : Local(-1002)
        }
    }

    sealed class FavouritePhotoErrors(value: Int) : ErrorCode(value) {
        sealed class Remote(value: Int) : FavouritePhotoErrors(value) {
            class UnknownError : Remote(-1)
            class Ok : Remote(0)
            class AlreadyFavourited : Remote(1)
            class BadRequest : Remote(2)
        }

        sealed class Local(value: Int) : FavouritePhotoErrors(value) {
            class BadServerResponse(val message: String? = null) : Local(-1000)
            class Timeout : Local(-1001)
        }
    }

    sealed class ReportPhotoErrors(value: Int) : ErrorCode(value) {
        sealed class Remote(value: Int) : ReportPhotoErrors(value) {
            class UnknownError : Remote(-1)
            class Ok : Remote(0)
            class AlreadyReported : Remote(1)
            class BadRequest : Remote(2)
        }

        sealed class Local(value: Int) : ReportPhotoErrors(value) {
            class BadServerResponse(val message: String? = null) : Local(-1000)
            class Timeout : Local(-1001)
        }
    }

    sealed class GetUserIdError(value: Int) : ErrorCode(value) {
        sealed class Remote(value: Int) : GetUserIdError(value) {
            class UnknownError : Remote(-1)
            class Ok : Remote(0)
            class DatabaseError : Remote(1)
        }

        sealed class Local(value: Int) : GetUserIdError(value) {
            class BadServerResponse(val message: String? = null) : Local(-1000)
            class Timeout : Local(-1001)
            class DatabaseError : Local(-1002)
        }
    }

    sealed class GetUploadedPhotoIdsError(value: Int) : ErrorCode(value) {
        sealed class Remote(value: Int) : GetUploadedPhotoIdsError(value) {
            class UnknownError : Remote(-1)
            class Ok : Remote(0)
            class DatabaseError : Remote(1)
            class BadRequest : Remote(2)
        }

        sealed class Local(value: Int) : GetUploadedPhotoIdsError(value) {
            class BadServerResponse(val message: String? = null) : Local(-1000)
            class Timeout : Local(-1001)
            class DatabaseError : Local(-1002)
        }
    }

    sealed class GetUploadedPhotosError(value: Int) : ErrorCode(value) {
        sealed class Remote(value: Int) : GetUploadedPhotosError(value) {
            class UnknownError : Remote(-1)
            class Ok : Remote(0)
            class DatabaseError : Remote(1)
            class BadRequest : Remote(2)
        }

        sealed class Local(value: Int) : GetUploadedPhotosError(value) {
            class BadServerResponse(val message: String? = null) : Local(-1000)
            class Timeout : Local(-1001)
            class DatabaseError : Local(-1002)
        }
    }

    //local
    sealed class TakePhotoErrors(value: Int) : ErrorCode(value) {
        class UnknownError : TakePhotoErrors(-1)
        class Ok(val photo: TakenPhoto) : TakePhotoErrors(0)
        class CameraIsNotAvailable : TakePhotoErrors(1)
        class CameraIsNotStartedException : TakePhotoErrors(2)
        class TimeoutException : TakePhotoErrors(3)
        class DatabaseError : TakePhotoErrors(4)
        class CouldNotTakePhoto : TakePhotoErrors(5)
    }

    companion object {

        //TODO: don't forget to add errorCodes here
        fun <T> fromInt(clazz: Class<*>, errorCodeInt: Int?): T {
            return when (clazz) {
                UploadPhotoErrors::class.java -> {
                    val errorCode = when (errorCodeInt) {
                        //local errors
                        null,
                        -1000 -> UploadPhotoErrors.Local.BadServerResponse()
                        -1001 -> UploadPhotoErrors.Local.NoPhotoFileOnDisk()
                        -1002 -> UploadPhotoErrors.Local.Timeout()
                        -1003 -> UploadPhotoErrors.Local.Interrupted()

                        //remote errors
                        -1 -> UploadPhotoErrors.Remote.UnknownError()
                        0 -> UploadPhotoErrors.Remote.Ok()
                        1 -> UploadPhotoErrors.Remote.BadRequest()
                        2 -> UploadPhotoErrors.Remote.DatabaseError()
                        else -> throw IllegalArgumentException("Unknown errorCodeInt $errorCodeInt")
                    }

                    errorCode as T
                }

                ReceivePhotosErrors::class.java -> {
                    val errorCode = when (errorCodeInt) {
                        //local errors
                        null,
                        -1000 -> ReceivePhotosErrors.Local.BadServerResponse()
                        -1001 -> ReceivePhotosErrors.Local.Timeout()

                        //remote errors
                        -1 -> ReceivePhotosErrors.Remote.UnknownError()
                        0 -> ReceivePhotosErrors.Remote.Ok()
                        1 -> ReceivePhotosErrors.Remote.BadRequest()
                        2 -> ReceivePhotosErrors.Remote.DatabaseError()
                        3 -> ReceivePhotosErrors.Remote.NoPhotosInRequest()
                        4 -> ReceivePhotosErrors.Remote.TooManyPhotosRequested()
                        5 -> ReceivePhotosErrors.Remote.NoPhotosToSendBack()
                        6 -> ReceivePhotosErrors.Remote.NotEnoughPhotosUploaded()
                        else -> throw IllegalArgumentException("Unknown errorCodeInt $errorCodeInt")
                    }

                    errorCode as T
                }

                GetGalleryPhotosErrors::class.java -> {
                    val errorCode = when (errorCodeInt) {
                        //local errors
                        null,
                        -1000 -> GetGalleryPhotosErrors.Local.BadServerResponse()
                        -1001 -> GetGalleryPhotosErrors.Local.Timeout()

                        //remote errors
                        -1 -> GetGalleryPhotosErrors.Remote.UnknownError()
                        0 -> GetGalleryPhotosErrors.Remote.Ok()
                        1 -> GetGalleryPhotosErrors.Remote.BadRequest()
                        else -> throw IllegalArgumentException("Unknown errorCodeInt $errorCodeInt")
                    }

                    errorCode as T
                }

                FavouritePhotoErrors::class.java -> {
                    val errorCode = when (errorCodeInt) {
                        //local errors
                        null,
                        -1000 -> FavouritePhotoErrors.Local.BadServerResponse()
                        -1001 -> FavouritePhotoErrors.Local.Timeout()

                        //remote errors
                        -1 -> FavouritePhotoErrors.Remote.UnknownError()
                        0 -> FavouritePhotoErrors.Remote.Ok()
                        1 -> FavouritePhotoErrors.Remote.AlreadyFavourited()
                        2 -> FavouritePhotoErrors.Remote.BadRequest()
                        else -> throw IllegalArgumentException("Unknown errorCodeInt $errorCodeInt")
                    }

                    errorCode as T
                }

                ReportPhotoErrors::class.java -> {
                    val errorCode = when (errorCodeInt) {
                        //local errors
                        null,
                        -1000 -> ReportPhotoErrors.Local.BadServerResponse()
                        -1001 -> ReportPhotoErrors.Local.Timeout()

                        //remote errors
                        -1 -> ReportPhotoErrors.Remote.UnknownError()
                        0 -> ReportPhotoErrors.Remote.Ok()
                        1 -> ReportPhotoErrors.Remote.AlreadyReported()
                        2 -> ReportPhotoErrors.Remote.BadRequest()
                        else -> throw IllegalArgumentException("Unknown errorCodeInt $errorCodeInt")
                    }

                    errorCode as T
                }

                else -> throw IllegalArgumentException("Unknown response class $clazz")
            }
        }
    }
}