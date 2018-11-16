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

fun Long.kb(): Long {
  return this * 1024L
}

fun Long.mb(): Long {
  return this.kb() * 1024L
}