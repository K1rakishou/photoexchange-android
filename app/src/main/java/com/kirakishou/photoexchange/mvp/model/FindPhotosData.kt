package com.kirakishou.photoexchange.mvp.model

class FindPhotosData(val userId: String,
                     val photoNames: String) {
  fun isUserIdEmpty(): Boolean {
    return userId.isEmpty()
  }

  fun isPhotoNamesEmpty(): Boolean {
    return photoNames.isEmpty()
  }
}