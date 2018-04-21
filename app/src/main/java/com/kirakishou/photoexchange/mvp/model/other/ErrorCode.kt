package com.kirakishou.photoexchange.mvp.model.other

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
            class BadServerResponse : Local(-1000)
            class NoPhotoFileOnDisk : Local(-1001)
        }
    }

    sealed class GetPhotoAnswerErrors(value: Int) : ErrorCode(value) {
        sealed class Remote(value: Int) : GetPhotoAnswerErrors(value) {
            class UnknownError : Remote(-1)
            class Ok : Remote(0)
            class BadRequest : Remote(1)
            class DatabaseError : Remote(2)
            class NoPhotosInRequest : Remote(3)
            class TooManyPhotosRequested : Remote(4)
            class NoPhotosToSendBack : Remote(5)
            class NotEnoughPhotosUploaded : Remote(6)
        }

        sealed class Local(value: Int) : GetPhotoAnswerErrors(value) {
            class BadServerResponse : Local(-1000)
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
            class BadServerResponse : Local(-1000)
        }
    }

    companion object {
        inline fun <reified T> fromInt(errorCodeInt: Int?): T {
            when (T::class.java) {
                UploadPhotoErrors::class.java -> {
                    val errorCode = when (errorCodeInt) {
                        //local errors
                        null,
                        -1000 -> UploadPhotoErrors.Local.BadServerResponse()
                        -1001 -> UploadPhotoErrors.Local.NoPhotoFileOnDisk()

                        //remote errors
                        -1 -> UploadPhotoErrors.Remote.UnknownError()
                        0 -> UploadPhotoErrors.Remote.Ok()
                        1 -> UploadPhotoErrors.Remote.BadRequest()
                        2 -> UploadPhotoErrors.Remote.DatabaseError()
                        else -> throw IllegalArgumentException("Unknown errorCodeInt $errorCodeInt")
                    }

                    return errorCode as T
                }

                GetPhotoAnswerErrors::class.java -> {
                    val errorCode = when (errorCodeInt) {
                        //local errors
                        null,
                        -1000 -> GetPhotoAnswerErrors.Local.BadServerResponse()

                        //remote errors
                        -1 -> GetPhotoAnswerErrors.Remote.UnknownError()
                        0 -> GetPhotoAnswerErrors.Remote.Ok()
                        1 -> GetPhotoAnswerErrors.Remote.BadRequest()
                        2 -> GetPhotoAnswerErrors.Remote.DatabaseError()
                        3 -> GetPhotoAnswerErrors.Remote.NoPhotosInRequest()
                        4 -> GetPhotoAnswerErrors.Remote.TooManyPhotosRequested()
                        5 -> GetPhotoAnswerErrors.Remote.NoPhotosToSendBack()
                        6 -> GetPhotoAnswerErrors.Remote.NotEnoughPhotosUploaded()
                        else -> throw IllegalArgumentException("Unknown errorCodeInt $errorCodeInt")
                    }

                    return errorCode as T
                }

                MarkPhotoAsReceivedErrors::class.java -> {
                    val errorCode = when (errorCodeInt) {
                        //local errors
                        null,
                        -1000 -> MarkPhotoAsReceivedErrors.Local.BadServerResponse()

                        //remote errors
                        -1 -> MarkPhotoAsReceivedErrors.Remote.UnknownError()
                        0 -> MarkPhotoAsReceivedErrors.Remote.Ok()
                        1 -> MarkPhotoAsReceivedErrors.Remote.BadRequest()
                        2 -> MarkPhotoAsReceivedErrors.Remote.BadPhotoId()
                        else -> throw IllegalArgumentException("Unknown errorCodeInt $errorCodeInt")
                    }

                    return errorCode as T
                }

                else -> throw IllegalArgumentException("Unknown response class ${T::class.java}")
            }
        }
    }
}