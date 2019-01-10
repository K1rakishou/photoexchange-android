package com.kirakishou.photoexchange.helper

enum class PhotosVisibility(val value: Int) {
  AlwaysPublic(0),
  AlwaysPrivate(1),
  AskMeEveryTime(2); //default setting

  companion object {
    fun fromBoolean(makePublic: Boolean?): PhotosVisibility {
      return when (makePublic) {
        null -> AskMeEveryTime
        true -> AlwaysPublic
        false -> AlwaysPrivate
      }
    }

    fun fromInt(value: Int?): PhotosVisibility {
      return when (value) {
        0 -> AlwaysPublic
        1 -> AlwaysPrivate
        else -> AskMeEveryTime
      }
    }
  }
}