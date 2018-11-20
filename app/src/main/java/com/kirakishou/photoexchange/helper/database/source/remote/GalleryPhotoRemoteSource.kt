package com.kirakishou.photoexchange.helper.database.source.remote

import com.kirakishou.photoexchange.helper.api.ApiClient
import net.response.GalleryPhotosResponse

class GalleryPhotoRemoteSource(
  private val apiClient: ApiClient
) {

  suspend fun getPageOfGalleryPhotos(lastUploadedOn: Long, count: Int): List<GalleryPhotosResponse.GalleryPhotoResponseData> {
    return apiClient.getPageOfGalleryPhotos(lastUploadedOn, count)
  }
}