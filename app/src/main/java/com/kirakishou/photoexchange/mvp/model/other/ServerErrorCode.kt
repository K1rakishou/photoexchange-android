package com.kirakishou.photoexchange.mvp.model.other

/**
 * Created by kirakishou on 7/26/2017.
 */
enum class ServerErrorCode(val value: Int) {
    BAD_ERROR_CODE(-2),
    UNKNOWN_ERROR(-1),
    OK(0),
    NO_PHOTO_FILE_ON_DISK(1),
    BAD_SERVER_RESPONSE(2);

    companion object {
        fun from(value: Int?): ServerErrorCode {
            if (value == null) {
                return BAD_ERROR_CODE
            }

            for (code in ServerErrorCode.values()) {
                if (code.value == value) {
                    return code
                }
            }

            throw IllegalArgumentException("Unknown value: $value")
        }
    }
}