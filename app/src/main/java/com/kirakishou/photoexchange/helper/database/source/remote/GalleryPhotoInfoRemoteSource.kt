package com.kirakishou.photoexchange.helper.database.source.remote

import com.kirakishou.photoexchange.helper.api.ApiClient
import net.response.GalleryPhotoInfoResponse

class GalleryPhotoInfoRemoteSource(
  private val apiClient: ApiClient
) {

  suspend fun getGalleryPhotoInfo(
    userId: String,
    galleryPhotoIds: String
  ): List<GalleryPhotoInfoResponse.GalleryPhotosInfoResponseData> {
    return apiClient.getGalleryPhotoInfo(userId, galleryPhotoIds)
  }
}