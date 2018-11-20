package com.kirakishou.photoexchange.helper.database.source.remote

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.api.response.FavouritePhotoResponseData

open class FavouritePhotoRemoteSource(
  private val apiClient: ApiClient
) {

  open suspend fun favouritePhoto(
    userId: String,
    photoName: String
  ): FavouritePhotoResponseData {
    return apiClient.favouritePhoto(userId, photoName)
  }

}