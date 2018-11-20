package com.kirakishou.photoexchange.helper.database.source.remote

import com.kirakishou.photoexchange.helper.api.ApiClient
import net.response.GetUploadedPhotosResponse

class GetUploadedPhotosRemoteSource(
  private val apiClient: ApiClient
) {

  suspend fun getPage(
    userId: String,
    lastUploadedOn: Long,
    count: Int
  ): List<GetUploadedPhotosResponse.UploadedPhotoResponseData> {
    return apiClient.getPageOfUploadedPhotos(userId, lastUploadedOn, count)
  }
}