package com.kirakishou.photoexchange.mvvm.model

/**
 * Created by kirakishou on 7/26/2017.
 */
enum class ErrorCode(val value: Int) {

    REC_OK(0);

    companion object {
        fun from(value: Int): ErrorCode {
            for (code in ErrorCode.values()) {
                if (code.value == value) {
                    return code
                }
            }

            throw IllegalArgumentException("Unknown value: $value")
        }
    }
}