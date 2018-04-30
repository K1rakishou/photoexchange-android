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

    sealed class FindPhotoAnswerErrors(value: Int) : ErrorCode(value) {
        sealed class Remote(value: Int) : FindPhotoAnswerErrors(value) {
            class UnknownError : Remote(-1)
            class Ok : Remote(0)
            class BadRequest : Remote(1)
            class DatabaseError : Remote(2)
            class NoPhotosInRequest : Remote(3)
            class TooManyPhotosRequested : Remote(4)
            class NoPhotosToSendBack : Remote(5)
            class NotEnoughPhotosUploaded : Remote(6)
        }

        sealed class Local(value: Int) : FindPhotoAnswerErrors(value) {
            class BadServerResponse(val message: String? = null) : Local(-1000)
            class Timeout : Local(-1001)
        }
    }

    sealed class MarkPhotoAsReceivedErrors(value: Int) : ErrorCode(value) {
        sealed class Remote(value: Int) : MarkPhotoAsReceivedErrors(value) {
            class UnknownError : Remote(-1)
            class Ok : Remote(0)
            class BadRequest : Remote(1)
            class BadPhotoId : Remote(2)
        }

        sealed class Local(value: Int) : MarkPhotoAsReceivedErrors(value) {
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
        inline fun <reified T> fromInt(errorCodeInt: Int?): T {
            return when (T::class.java) {
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

                FindPhotoAnswerErrors::class.java -> {
                    val errorCode = when (errorCodeInt) {
                        //local errors
                        null,
                        -1000 -> FindPhotoAnswerErrors.Local.BadServerResponse()
                        -1001 -> FindPhotoAnswerErrors.Local.Timeout()

                        //remote errors
                        -1 -> FindPhotoAnswerErrors.Remote.UnknownError()
                        0 -> FindPhotoAnswerErrors.Remote.Ok()
                        1 -> FindPhotoAnswerErrors.Remote.BadRequest()
                        2 -> FindPhotoAnswerErrors.Remote.DatabaseError()
                        3 -> FindPhotoAnswerErrors.Remote.NoPhotosInRequest()
                        4 -> FindPhotoAnswerErrors.Remote.TooManyPhotosRequested()
                        5 -> FindPhotoAnswerErrors.Remote.NoPhotosToSendBack()
                        6 -> FindPhotoAnswerErrors.Remote.NotEnoughPhotosUploaded()
                        else -> throw IllegalArgumentException("Unknown errorCodeInt $errorCodeInt")
                    }

                    errorCode as T
                }

                MarkPhotoAsReceivedErrors::class.java -> {
                    val errorCode = when (errorCodeInt) {
                        //local errors
                        null,
                        -1000 -> MarkPhotoAsReceivedErrors.Local.BadServerResponse()
                        -1001 -> MarkPhotoAsReceivedErrors.Local.Timeout()

                        //remote errors
                        -1 -> MarkPhotoAsReceivedErrors.Remote.UnknownError()
                        0 -> MarkPhotoAsReceivedErrors.Remote.Ok()
                        1 -> MarkPhotoAsReceivedErrors.Remote.BadRequest()
                        2 -> MarkPhotoAsReceivedErrors.Remote.BadPhotoId()
                        else -> throw IllegalArgumentException("Unknown errorCodeInt $errorCodeInt")
                    }

                    errorCode as T
                }

                else -> throw IllegalArgumentException("Unknown response class ${T::class.java}")
            }
        }
    }
}