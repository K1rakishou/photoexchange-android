package com.kirakishou.photoexchange.mvp.model.other

import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import kotlin.reflect.KClass

/**
 * Created by kirakishou on 7/26/2017.
 */

sealed class ErrorCode(private val _value: Int) {

    fun getValue(): Int {
        return _value + offset()
    }

    abstract fun offset(): Int

    sealed class TakePhotoErrors(value: Int) : ErrorCode(value) {
        override fun offset(): Int = -50

        class UnknownError : TakePhotoErrors(0)
        class Ok(val photo: TakenPhoto) : TakePhotoErrors(1)
        class CameraIsNotAvailable : TakePhotoErrors(2)
        class CameraIsNotStartedException : TakePhotoErrors(3)
        class TimeoutException : TakePhotoErrors(4)
        class DatabaseError : TakePhotoErrors(5)
        class CouldNotTakePhoto : TakePhotoErrors(6)
    }

    sealed class UploadPhotoErrors(value: Int) : ErrorCode(value) {
        override fun offset(): Int = 50

        class UnknownError : UploadPhotoErrors(0)
        class Ok : UploadPhotoErrors(1)
        class BadRequest : UploadPhotoErrors(2)
        class DatabaseError : UploadPhotoErrors(3)

        class LocalBadServerResponse : UploadPhotoErrors(25)
        class LocalNoPhotoFileOnDisk : UploadPhotoErrors(26)
        class LocalTimeout : UploadPhotoErrors(27)
        class LocalInterrupted : UploadPhotoErrors(28)
        class LocalDatabaseError : UploadPhotoErrors(29)
        class LocalCouldNotRotatePhoto : UploadPhotoErrors(30)
        class LocalCouldNotGetUserId : UploadPhotoErrors(31)
        class LocalCouldNotUpdatePhotoState : UploadPhotoErrors(32)

        companion object {
            fun fromInt(value: Int): UploadPhotoErrors {
                return when (value) {
                    50 -> UnknownError()
                    51 -> Ok()
                    52 -> BadRequest()
                    53 -> DatabaseError()

                    75 -> LocalBadServerResponse()
                    76 -> LocalNoPhotoFileOnDisk()
                    77 -> LocalTimeout()
                    78 -> LocalInterrupted()
                    79 -> LocalDatabaseError()
                    80 -> LocalCouldNotRotatePhoto()
                    81 -> LocalCouldNotGetUserId()
                    82 -> LocalCouldNotUpdatePhotoState()
                    else -> throw IllegalArgumentException("Unknown value $value")
                }
            }
        }
    }

    sealed class ReceivePhotosErrors(value: Int) : ErrorCode(value) {
        override fun offset(): Int = 100

        class UnknownError : ReceivePhotosErrors(0)
        class Ok : ReceivePhotosErrors(1)
        class BadRequest : ReceivePhotosErrors(2)
        class NoPhotosInRequest : ReceivePhotosErrors(3)
        class NotEnoughPhotosOnServer : ReceivePhotosErrors(4)

        class LocalDatabaseError : ReceivePhotosErrors(25)
        class LocalNotEnoughPhotosUploaded : ReceivePhotosErrors(26)
        class LocalBadServerResponse : ReceivePhotosErrors(27)
        class LocalTimeout : ReceivePhotosErrors(28)
        class LocalCouldNotGetUserId : ReceivePhotosErrors(29)
        class LocalUserIdIsEmpty : ReceivePhotosErrors(30)
        class LocalPhotoNamesAreEmpty : ReceivePhotosErrors(31)

        companion object {
            fun fromInt(value: Int): ReceivePhotosErrors {
                return when (value) {
                    100 -> UnknownError()
                    101 -> Ok()
                    102 -> BadRequest()
                    103 -> NoPhotosInRequest()
                    104 -> NotEnoughPhotosOnServer()

                    125 -> LocalDatabaseError()
                    126 -> LocalNotEnoughPhotosUploaded()
                    127 -> LocalBadServerResponse()
                    128 -> LocalTimeout()
                    129 -> LocalCouldNotGetUserId()
                    130 -> LocalUserIdIsEmpty()
                    131 -> LocalPhotoNamesAreEmpty()
                    else -> throw IllegalArgumentException("Unknown value $value")
                }
            }
        }
    }

    sealed class GetGalleryPhotosErrors(value: Int) : ErrorCode(value) {
        override fun offset(): Int = 150

        class UnknownError : GetGalleryPhotosErrors(0)
        class Ok : GetGalleryPhotosErrors(1)
        class BadRequest : GetGalleryPhotosErrors(2)
        class NoPhotosInRequest : GetGalleryPhotosErrors(3)

        class LocalBadServerResponse : GetGalleryPhotosErrors(25)
        class LocalTimeout : GetGalleryPhotosErrors(26)
        class LocalDatabaseError : GetGalleryPhotosErrors(27)

        companion object {
            fun fromInt(value: Int): GetGalleryPhotosErrors {
                return when (value) {
                    150 -> UnknownError()
                    151 -> Ok()
                    152 -> BadRequest()
                    153 -> NoPhotosInRequest()

                    175 -> LocalBadServerResponse()
                    176 -> LocalTimeout()
                    177 -> LocalDatabaseError()
                    else -> throw IllegalArgumentException("Unknown value $value")
                }
            }
        }
    }

    sealed class FavouritePhotoErrors(value: Int) : ErrorCode(value) {
        override fun offset(): Int = 200

        class UnknownError : FavouritePhotoErrors(0)
        class Ok : FavouritePhotoErrors(1)
        class BadRequest : FavouritePhotoErrors(2)

        class LocalBadServerResponse : FavouritePhotoErrors(25)
        class LocalTimeout : FavouritePhotoErrors(26)

        companion object {
            fun fromInt(value: Int): FavouritePhotoErrors {
                return when (value) {
                    200 -> UnknownError()
                    201 -> Ok()
                    202 -> BadRequest()

                    225 -> LocalBadServerResponse()
                    226 -> LocalTimeout()
                    else -> throw IllegalArgumentException("Unknown value $value")
                }
            }
        }
    }

    sealed class ReportPhotoErrors(value: Int) : ErrorCode(value) {
        override fun offset(): Int = 250

        class UnknownError : ReportPhotoErrors(0)
        class Ok : ReportPhotoErrors(1)
        class BadRequest : ReportPhotoErrors(2)

        class LocalBadServerResponse : ReportPhotoErrors(25)
        class LocalTimeout : ReportPhotoErrors(26)

        companion object {
            fun fromInt(value: Int): ReportPhotoErrors {
                return when (value) {
                    250 -> UnknownError()
                    251 -> Ok()
                    252 -> BadRequest()

                    275 -> LocalBadServerResponse()
                    276 -> LocalTimeout()
                    else -> throw IllegalArgumentException("Unknown value $value")
                }
            }
        }
    }

    sealed class GetUserIdError(value: Int) : ErrorCode(value) {
        override fun offset(): Int = 300

        class UnknownError : GetUserIdError(0)
        class Ok : GetUserIdError(1)
        class DatabaseError : GetUserIdError(2)

        class LocalBadServerResponse : GetUserIdError(25)
        class LocalTimeout : GetUserIdError(26)
        class LocalDatabaseError : GetUserIdError(27)

        companion object {
            fun fromInt(value: Int): GetUserIdError {
                return when (value) {
                    300 -> UnknownError()
                    301 -> Ok()
                    302 -> DatabaseError()

                    325 -> LocalBadServerResponse()
                    326 -> LocalTimeout()
                    327 -> LocalDatabaseError()
                    else -> throw IllegalArgumentException("Unknown value $value")
                }
            }
        }
    }

    sealed class GetUploadedPhotosErrors(value: Int) : ErrorCode(value) {
        override fun offset(): Int = 350

        class UnknownError : GetUploadedPhotosErrors(0)
        class Ok : GetUploadedPhotosErrors(1)
        class DatabaseError : GetUploadedPhotosErrors(2)
        class BadRequest : GetUploadedPhotosErrors(3)
        class NoPhotosInRequest : GetUploadedPhotosErrors(4)

        class LocalBadServerResponse : GetUploadedPhotosErrors(25)
        class LocalTimeout : GetUploadedPhotosErrors(26)
        class LocalUserIdIsEmpty : GetUploadedPhotosErrors(27)

        companion object {
            fun fromInt(value: Int): GetUploadedPhotosErrors {
                return when (value) {
                    350 -> UnknownError()
                    351 -> Ok()
                    352 -> DatabaseError()
                    353 -> BadRequest()
                    354 -> NoPhotosInRequest()

                    375 -> LocalBadServerResponse()
                    376 -> LocalTimeout()
                    377 -> LocalUserIdIsEmpty()
                    else -> throw IllegalArgumentException("Unknown value $value")
                }
            }
        }
    }

    sealed class GetReceivedPhotosErrors(value: Int) : ErrorCode(value) {
        override fun offset(): Int = 400

        class UnknownError : GetReceivedPhotosErrors(0)
        class Ok : GetReceivedPhotosErrors(1)
        class DatabaseError : GetReceivedPhotosErrors(2)
        class BadRequest : GetReceivedPhotosErrors(3)
        class NoPhotosInRequest : GetReceivedPhotosErrors(4)

        class LocalBadServerResponse : GetReceivedPhotosErrors(25)
        class LocalTimeout : GetReceivedPhotosErrors(26)
        class LocalUserIdIsEmpty : GetReceivedPhotosErrors(27)

        companion object {
            fun fromInt(value: Int): GetReceivedPhotosErrors {
                return when (value) {
                    400 -> UnknownError()
                    401 -> Ok()
                    402 -> DatabaseError()
                    403 -> BadRequest()
                    404 -> NoPhotosInRequest()

                    425 -> LocalBadServerResponse()
                    426 -> LocalTimeout()
                    427 -> LocalUserIdIsEmpty()
                    else -> throw IllegalArgumentException("Unknown value $value")
                }
            }
        }
    }

    sealed class CheckAccountExistsErrors(value: Int) : ErrorCode(value) {
        override fun offset(): Int = 450

        class UnknownError : CheckAccountExistsErrors(0)
        class Ok : CheckAccountExistsErrors(1)

        class LocalDatabaseError : CheckAccountExistsErrors(25)
        class LocalTimeout : CheckAccountExistsErrors(26)

        companion object {
            fun fromInt(value: Int): CheckAccountExistsErrors {
                return when (value) {
                    450 -> UnknownError()
                    451 -> Ok()

                    475 -> LocalDatabaseError()
                    476 -> LocalTimeout()
                    else -> throw IllegalArgumentException("Unknown value $value")
                }
            }
        }
    }

    companion object {
        fun <T : ErrorCode> fromInt(clazz: KClass<*>, errorCodeInt: Int): T {
            val errorCode = when (clazz) {
                UploadPhotoErrors::class -> UploadPhotoErrors.fromInt(errorCodeInt)
                ReceivePhotosErrors::class -> ReceivePhotosErrors.fromInt(errorCodeInt)
                GetGalleryPhotosErrors::class -> GetGalleryPhotosErrors.fromInt(errorCodeInt)
                FavouritePhotoErrors::class -> FavouritePhotoErrors.fromInt(errorCodeInt)
                ReportPhotoErrors::class -> ReportPhotoErrors.fromInt(errorCodeInt)
                GetUserIdError::class -> GetUserIdError.fromInt(errorCodeInt)
                GetUploadedPhotosErrors::class -> GetUploadedPhotosErrors.fromInt(errorCodeInt)
                GetReceivedPhotosErrors::class -> GetReceivedPhotosErrors.fromInt(errorCodeInt)
                else -> throw IllegalArgumentException("Unknown class  $clazz")
            }

            return errorCode as T
        }
    }
}