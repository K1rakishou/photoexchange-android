package com.kirakishou.photoexchange.helper

/**
 * This setting tells what we can do when there is no wifi connection:
 * Neither means that we can't access network at all.
 * CanAccessInternet means that we can make lightweight requests (text/jsons)
 * CanLoadImages means that we can make requests and event load images
 * */
enum class NetworkAccessLevel(val value: Int) {
  Neither(0),
  CanAccessInternet(1), //default setting
  CanLoadImages(2);

  companion object {
    fun fromInt(value: Int?): NetworkAccessLevel {
      if (value == null) {
        return CanAccessInternet
      }

      return NetworkAccessLevel.values().first { it.value == value }
    }
  }
}