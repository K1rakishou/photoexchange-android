package com.kirakishou.photoexchange.mvp.model.other


/**
 * Created by kirakishou on 7/26/2017.
 */

enum class ErrorCode(val value: Int) {
  UnknownError(0),
  Ok(1),
  BadRequest(2),
  DatabaseError(3),
  CameraIsNotAvailable(4),
  CameraIsNotStartedException(5),
  TimeoutException(6),
  CouldNotTakePhoto(7),
  NoPhotosInRequest(8),
  NoPhotosToSendBack(9);

  fun getErrorMessage(): String {
    TODO()
  }

  companion object {
    fun fromInt(value: Int): ErrorCode {
      return ErrorCode.values().first { it.value == value }
    }
  }
}