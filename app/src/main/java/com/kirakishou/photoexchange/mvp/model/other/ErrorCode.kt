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
        class LocalCouldNotGetUserId : UploadPhotoErrors(30)
        class CouldNotRotatePhoto : UploadPhotoErrors(31)

        companion object {
            fun fromInt(value: Int): UploadPhotoErrors {
                return when (value) {
                    50 -> UnknownError()
                    51 -> Ok()
                    52 -> BadRequest()
                    53 -> DatabaseError()

                    75 -> LocalBadServerResponse()
                    66 -> LocalNoPhotoFileOnDisk()
                    77 -> LocalTimeout()
                    78 -> LocalInterrupted()
                    79 -> LocalDatabaseError()
                    800 -> LocalCouldNotGetUserId()
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
        class NoPhotosToSendBack : ReceivePhotosErrors(4)

        class LocalDatabaseError : ReceivePhotosErrors(25)
        class LocalTooManyPhotosRequested : ReceivePhotosErrors(26)
        class LocalNotEnoughPhotosUploaded : ReceivePhotosErrors(27)
        class LocalBadServerResponse : ReceivePhotosErrors(28)
        class LocalTimeout : ReceivePhotosErrors(29)

        companion object {
            fun fromInt(value: Int): ReceivePhotosErrors {
                return when (value) {
                    100 -> UnknownError()
                    101 -> Ok()
                    102 -> BadRequest()
                    103 -> NoPhotosInRequest()
                    104 -> NoPhotosToSendBack()

                    125 -> LocalDatabaseError()
                    126 -> LocalTooManyPhotosRequested()
                    127 -> LocalNotEnoughPhotosUploaded()
                    128 -> LocalBadServerResponse()
                    129 -> LocalTimeout()
                    else -> throw IllegalArgumentException("Unknown value $value")
                }
            }
        }
    }

    sealed class GalleryPhotosErrors(value: Int) : ErrorCode(value) {
        override fun offset(): Int = 150

        class UnknownError : GalleryPhotosErrors(0)
        class Ok : GalleryPhotosErrors(1)
        class BadRequest : GalleryPhotosErrors(2)
        class NoPhotosInRequest : GalleryPhotosErrors(3)

        class LocalBadServerResponse : GalleryPhotosErrors(25)
        class LocalTimeout : GalleryPhotosErrors(26)
        class LocalDatabaseError : GalleryPhotosErrors(27)

        companion object {
            fun fromInt(value: Int): GalleryPhotosErrors {
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

    companion object {
        fun fromInt(clazz: KClass<*>, errorCodeInt: Int): ErrorCode {
            return when (clazz) {
                UploadPhotoErrors::class -> UploadPhotoErrors.fromInt(errorCodeInt)
                ReceivePhotosErrors::class -> ReceivePhotosErrors.fromInt(errorCodeInt)
                GalleryPhotosErrors::class -> GalleryPhotosErrors.fromInt(errorCodeInt)
                FavouritePhotoErrors::class -> FavouritePhotoErrors.fromInt(errorCodeInt)
                ReportPhotoErrors::class -> ReportPhotoErrors.fromInt(errorCodeInt)
                GetUserIdError::class -> GetUserIdError.fromInt(errorCodeInt)
                GetUploadedPhotosErrors::class -> GetUploadedPhotosErrors.fromInt(errorCodeInt)
                GetReceivedPhotosErrors::class -> GetReceivedPhotosErrors.fromInt(errorCodeInt)
                else -> throw IllegalArgumentException("Unknown class  $clazz")
            }
        }
    }
}