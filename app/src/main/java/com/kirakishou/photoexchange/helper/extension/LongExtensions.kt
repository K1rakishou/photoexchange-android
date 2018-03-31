package com.kirakishou.photoexchange.helper.extension

fun Long.seconds(): Long {
    return this * 1000L
}

fun Long.minutes(): Long {
    return this.seconds() * 60L
}

fun Long.hours(): Long {
    return this.minutes() * 60L
}

fun Long.days(): Long {
    return this.hours() * 24L
}