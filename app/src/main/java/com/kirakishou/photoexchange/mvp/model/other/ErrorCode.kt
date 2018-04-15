package com.kirakishou.photoexchange.mvp.model.other

/**
 * Created by kirakishou on 7/26/2017.
 */

//sealed class ErrorCode(val value: Int) {
//
//    fun toInt(): Int {
//        return this.value
//    }
//
//    sealed class UploadPhotoErrors(value: Int) : ErrorCode(value) {
//        class UnknownError : UploadPhotoErrors(-1)
//        class Ok : UploadPhotoErrors(0)
//        class BadRequest : UploadPhotoErrors(1)
//        class DatabaseError : UploadPhotoErrors(2)
//    }
//
//    sealed class GetPhotoAnswerErrors(value: Int) : ErrorCode(value) {
//        class UnknownError : GetPhotoAnswerErrors(-1)
//        class Ok : GetPhotoAnswerErrors(0)
//        class BadRequest : GetPhotoAnswerErrors(1)
//        class DatabaseError : GetPhotoAnswerErrors(2)
//        class NoPhotosInRequest : GetPhotoAnswerErrors(3)
//        class TooManyPhotosRequested : GetPhotoAnswerErrors(4)
//        class NoPhotosToSendBack : GetPhotoAnswerErrors(5)
//        class NotEnoughPhotosUploaded : GetPhotoAnswerErrors(6)
//    }
//
//    sealed class MarkPhotoAsReceivedErrors(value: Int) : ErrorCode(value) {
//        class UnknownError : MarkPhotoAsReceivedErrors(-1)
//        class Ok : MarkPhotoAsReceivedErrors(0)
//        class BadRequest : MarkPhotoAsReceivedErrors(1)
//        class BadPhotoId : MarkPhotoAsReceivedErrors(2)
//    }
//}

enum class ErrorCode(val value: Int) {
    NO_PHOTO_FILE_ON_DISK(-4),
    BAD_SERVER_RESPONSE(-3),
    BAD_ERROR_CODE(-2),
    UNKNOWN_ERROR(-1),
    OK(0),
    BAD_REQUEST(1),
    REPOSITORY_ERROR(2),
    DISK_ERROR(3),
    NO_PHOTOS_TO_SEND_BACK(4),
    BAD_PHOTO_ID(5),
    UPLOAD_MORE_PHOTOS(6),
    NOT_FOUND(7);

    companion object {
        fun from(value: Int?): ErrorCode {
            if (value == null) {
                return BAD_ERROR_CODE
            }

            for (code in ErrorCode.values()) {
                if (code.value == value) {
                    return code
                }
            }

            throw IllegalArgumentException("Unknown value: $value")
        }
    }
}