package com.kirakishou.photoexchange.helper

/**
 * Created by kirakishou on 11/3/2017.
 */
class LonLat(val lon: Double,
             val lat: Double) {

  fun isEmpty(): Boolean {
    return lon == -1.0 && lat == -1.0
  }

  override fun toString(): String {
    return "[lon: $lon, lat: $lat]"
  }

  companion object {
    const val NO_LOCATION = -361.0
    const val DISABLED_LOCATION = -362.0

    fun empty(): LonLat {
      return LonLat(-1.0, -1.0)
    }
  }
}