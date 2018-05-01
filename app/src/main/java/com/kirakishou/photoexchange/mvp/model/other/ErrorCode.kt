package com.kirakishou.photoexchange.mvp.model.other

import com.kirakishou.photoexchange.mvp.model.MyPhoto

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
        }

        sealed class Local(value: Int) : UploadPhotoErrors(value) {
            class BadServerResponse(val message: String? = null) : Local(-1000)
            class NoPhotoFileOnDisk : Local(-1001)
            class Timeout : Local(-1002)
        }
    }

    sealed class GetPhotoAnswersErrors(value: Int) : ErrorCode(value) {
        sealed class Remote(value: Int) : GetPhotoAnswersErrors(value) {
            class UnknownError : Remote(-1)
            class Ok : Remote(0)
            class BadRequest : Remote(1)
            class DatabaseError : Remote(2)
            class NoPhotosInRequest : Remote(3)
            class TooManyPhotosRequested : Remote(4)
            class NoPhotosToSendBack : Remote(5)
            class NotEnoughPhotosUploaded : Remote(6)
        }

        sealed class Local(value: Int) : GetPhotoAnswersErrors(value) {
            class BadServerResponse(val message: String? = null) : Local(-1000)
            class Timeout : Local(-1001)
        }
    }

    sealed class GalleryPhotosErrors(value: Int) : ErrorCode(value) {
        sealed class Remote(value: Int) : GalleryPhotosErrors(value) {
            class UnknownError : Remote(-1)
            class Ok : Remote(0)
            class BadRequest : Remote(1)
        }

        sealed class Local(value: Int) : GalleryPhotosErrors(value) {
            class BadServerResponse(val message: String? = null) : Local(-1000)
            class Timeout : Local(-1001)
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

    //local
    sealed class TakePhotoErrors(value: Int) : ErrorCode(value) {
        class UnknownError : TakePhotoErrors(-1)
        class Ok(val photo: MyPhoto) : TakePhotoErrors(0)
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

                        //remote errors
                        -1 -> UploadPhotoErrors.Remote.UnknownError()
                        0 -> UploadPhotoErrors.Remote.Ok()
                        1 -> UploadPhotoErrors.Remote.BadRequest()
                        2 -> UploadPhotoErrors.Remote.DatabaseError()
                        else -> throw IllegalArgumentException("Unknown errorCodeInt $errorCodeInt")
                    }

                    errorCode as T
                }

                GetPhotoAnswersErrors::class.java -> {
                    val errorCode = when (errorCodeInt) {
                        //local errors
                        null,
                        -1000 -> GetPhotoAnswersErrors.Local.BadServerResponse()
                        -1001 -> GetPhotoAnswersErrors.Local.Timeout()

                        //remote errors
                        -1 -> GetPhotoAnswersErrors.Remote.UnknownError()
                        0 -> GetPhotoAnswersErrors.Remote.Ok()
                        1 -> GetPhotoAnswersErrors.Remote.BadRequest()
                        2 -> GetPhotoAnswersErrors.Remote.DatabaseError()
                        3 -> GetPhotoAnswersErrors.Remote.NoPhotosInRequest()
                        4 -> GetPhotoAnswersErrors.Remote.TooManyPhotosRequested()
                        5 -> GetPhotoAnswersErrors.Remote.NoPhotosToSendBack()
                        6 -> GetPhotoAnswersErrors.Remote.NotEnoughPhotosUploaded()
                        else -> throw IllegalArgumentException("Unknown errorCodeInt $errorCodeInt")
                    }

                    errorCode as T
                }

                GalleryPhotosErrors::class.java -> {
                    val errorCode = when (errorCodeInt) {
                        //local errors
                        null,
                        -1000 -> GalleryPhotosErrors.Local.BadServerResponse()
                        -1001 -> GalleryPhotosErrors.Local.Timeout()

                        //remote errors
                        -1 -> GalleryPhotosErrors.Remote.UnknownError()
                        0 -> GalleryPhotosErrors.Remote.Ok()
                        1 -> GalleryPhotosErrors.Remote.BadRequest()
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