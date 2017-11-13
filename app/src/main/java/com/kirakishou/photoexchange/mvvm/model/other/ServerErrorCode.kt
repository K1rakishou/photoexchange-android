package com.kirakishou.photoexchange.mvvm.model.other

/**
 * Created by kirakishou on 7/26/2017.
 */
enum class ServerErrorCode(val value: Int) {
    BAD_ERROR_CODE(-2),
    UNKNOWN_ERROR(-1),
    OK(0),
    BAD_REQUEST(1),
    REPOSITORY_ERROR(2),
    DISK_ERROR(3),
    NOTHING_FOUND(4);

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