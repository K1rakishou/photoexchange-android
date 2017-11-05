package com.kirakishou.photoexchange.mvvm.model

/**
 * Created by kirakishou on 7/26/2017.
 */
enum class ServerErrorCode(val value: Int) {

    REC_BAD_ERROR_CODE(-1),
    REC_OK(0);

    companion object {
        fun from(value: Int?): ServerErrorCode {
            if (value == null) {
                return REC_BAD_ERROR_CODE
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