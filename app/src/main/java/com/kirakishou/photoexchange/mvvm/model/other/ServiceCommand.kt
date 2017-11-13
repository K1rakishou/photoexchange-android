package com.kirakishou.photoexchange.mvvm.model.other

/**
 * Created by kirakishou on 11/4/2017.
 */

enum class ServiceCommand(val value: Int) {
    SEND_PHOTO(0),
    FIND_PHOTO(1);

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