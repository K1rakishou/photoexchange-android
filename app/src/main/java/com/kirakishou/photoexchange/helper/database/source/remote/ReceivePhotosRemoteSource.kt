package com.kirakishou.photoexchange.helper.database.source.remote

import com.kirakishou.photoexchange.helper.api.ApiClient
import net.response.ReceivedPhotosResponse

class ReceivePhotosRemoteSource(
  private val apiClient: ApiClient
) {

  suspend fun receivePhotos(
    userId: String,
    photoNames: String
  ): List<ReceivedPhotosResponse.ReceivedPhotoResponseData> {
    return apiClient.receivePhotos(userId, photoNames)
  }
}