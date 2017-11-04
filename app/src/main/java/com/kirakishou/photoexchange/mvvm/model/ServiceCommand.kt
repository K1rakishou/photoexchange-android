package com.kirakishou.photoexchange.mvvm.model

/**
 * Created by kirakishou on 11/4/2017.
 */

enum class ServiceCommand(val value: Int) {
    SEND_PHOTO(0);

    companion object {
        fun from(value: Int): ServiceCommand {
            for (code in ServiceCommand.values()) {
                if (code.value == value) {
                    return code
                }
            }

            throw IllegalArgumentException("Unknown value: $value")
        }
    }
}