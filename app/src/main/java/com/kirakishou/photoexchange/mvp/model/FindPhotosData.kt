package com.kirakishou.photoexchange.mvp.model

class FindPhotosData(val userUuid: String,
                     val photoNames: String) {
  fun isUserUuidEmpty(): Boolean {
    return userUuid.isEmpty()
  }

  fun isPhotoNamesEmpty(): Boolean {
    return photoNames.isEmpty()
  }
}