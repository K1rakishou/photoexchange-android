package com.kirakishou.photoexchange.helper.extension

fun Int.seconds(): Long {
    return this * 1000L
}

fun Int.minutes(): Long {
    return this.seconds() * 60L
}

fun Int.hours(): Long {
    return this.minutes() * 60L
}

fun Int.days(): Long {
    return this.hours() * 24L
}