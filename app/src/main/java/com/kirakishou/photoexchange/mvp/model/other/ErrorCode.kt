package com.kirakishou.photoexchange.mvp.model.other

/**
 * Created by kirakishou on 7/26/2017.
 */

sealed class ErrorCode(val value: Int) {

    fun toInt(): Int {
        return this.value
    }

    sealed class UploadPhotoErrors(value: Int) : ErrorCode(value) {
        class BadServerResponse : UploadPhotoErrors(-2)
        class UnknownError : UploadPhotoErrors(-1)
        class Ok : UploadPhotoErrors(0)
        class BadRequest : UploadPhotoErrors(1)
        class DatabaseError : UploadPhotoErrors(2)
        class NoPhotoFileOnDisk : UploadPhotoErrors(3)
    }

    sealed class GetPhotoAnswerErrors(value: Int) : ErrorCode(value) {
        class BadServerResponse : GetPhotoAnswerErrors(-2)
        class UnknownError : GetPhotoAnswerErrors(-1)
        class Ok : GetPhotoAnswerErrors(0)
        class BadRequest : GetPhotoAnswerErrors(1)
        class DatabaseError : GetPhotoAnswerErrors(2)
        class NoPhotosInRequest : GetPhotoAnswerErrors(3)
        class TooManyPhotosRequested : GetPhotoAnswerErrors(4)
        class NoPhotosToSendBack : GetPhotoAnswerErrors(5)
        class NotEnoughPhotosUploaded : GetPhotoAnswerErrors(6)
    }

    sealed class MarkPhotoAsReceivedErrors(value: Int) : ErrorCode(value) {
        class BadServerResponse : MarkPhotoAsReceivedErrors(-2)
        class UnknownError : MarkPhotoAsReceivedErrors(-1)
        class Ok : MarkPhotoAsReceivedErrors(0)
        class BadRequest : MarkPhotoAsReceivedErrors(1)
        class BadPhotoId : MarkPhotoAsReceivedErrors(2)
    }

    companion object {
        inline fun <reified T> fromInt(errorCodeInt: Int?): T {
            when (T::class.java) {
                UploadPhotoErrors::class.java -> {
                    val errorCode = when (errorCodeInt) {
                        null,
                        -2 -> UploadPhotoErrors.BadServerResponse()
                        -1 -> UploadPhotoErrors.UnknownError()
                        0 -> UploadPhotoErrors.Ok()
                        1 -> UploadPhotoErrors.BadRequest()
                        2 -> UploadPhotoErrors.DatabaseError()
                        3 -> UploadPhotoErrors.NoPhotoFileOnDisk()
                        else -> throw IllegalArgumentException("Unknown errorCodeInt $errorCodeInt")
                    }

                    return errorCode as T
                }

                GetPhotoAnswerErrors::class.java -> {
                    val errorCode =  when (errorCodeInt) {
                        null,
                        -2 -> GetPhotoAnswerErrors.BadServerResponse()
                        -1 -> GetPhotoAnswerErrors.UnknownError()
                        0 -> GetPhotoAnswerErrors.Ok()
                        1 -> GetPhotoAnswerErrors.BadRequest()
                        2 -> GetPhotoAnswerErrors.DatabaseError()
                        3 -> GetPhotoAnswerErrors.NoPhotosInRequest()
                        4 -> GetPhotoAnswerErrors.TooManyPhotosRequested()
                        5 -> GetPhotoAnswerErrors.NoPhotosToSendBack()
                        6 -> GetPhotoAnswerErrors.NotEnoughPhotosUploaded()
                        else -> throw IllegalArgumentException("Unknown errorCodeInt $errorCodeInt")
                    }

                    return errorCode as T
                }

                MarkPhotoAsReceivedErrors::class.java -> {
                    val errorCode = when (errorCodeInt) {
                        null,
                        -2 -> MarkPhotoAsReceivedErrors.BadServerResponse()
                        -1 -> MarkPhotoAsReceivedErrors.UnknownError()
                        0 -> MarkPhotoAsReceivedErrors.Ok()
                        1 -> MarkPhotoAsReceivedErrors.BadRequest()
                        2 -> MarkPhotoAsReceivedErrors.BadPhotoId()
                        else -> throw IllegalArgumentException("Unknown errorCodeInt $errorCodeInt")
                    }

                    return errorCode as T
                }

                else -> throw IllegalArgumentException("Unknown response class ${T::class.java}")
            }
        }
    }
}