package com.kirakishou.photoexchange.helper.database.source.remote

import com.kirakishou.photoexchange.helper.api.ApiClient
import net.response.ReceivedPhotosResponse

class GetReceivedPhotosRemoteSource(
  private val apiClient: ApiClient
) {
  suspend fun getReceivedPhotos(
    userId: String,
    lastUploadedOn: Long,
    count: Int
  ): List<ReceivedPhotosResponse.ReceivedPhotoResponseData> {
    return apiClient.getReceivedPhotos(userId, lastUploadedOn, count)
  }
}