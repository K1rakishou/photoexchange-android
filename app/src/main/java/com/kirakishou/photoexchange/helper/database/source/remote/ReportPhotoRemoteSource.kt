package com.kirakishou.photoexchange.helper.database.source.remote

import com.kirakishou.photoexchange.helper.api.ApiClient

open class ReportPhotoRemoteSource(
  private val apiClient: ApiClient
) {

  suspend fun reportPhoto(userId: String, photoName: String): Boolean {
    return apiClient.reportPhoto(userId, photoName)
  }
}