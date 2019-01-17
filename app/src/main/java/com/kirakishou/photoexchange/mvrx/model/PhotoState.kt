package com.kirakishou.photoexchange.mvrx.model

/**
 * Created by kirakishou on 3/8/2018.
 */
enum class PhotoState(val state: Int) {
  PHOTO_TAKEN(0),
  PHOTO_QUEUED_UP(1),
  PHOTO_UPLOADING(2);

  companion object {
    fun from(state: Int): PhotoState {
      val result = PhotoState.values().firstOrNull { it.state == state }
      if (result == null) {
        throw RuntimeException("Unknown state $state")
      }

      return result
    }
  }
}