package com.kirakishou.photoexchange.helper.api.request

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.gson.JsonConverter
import com.kirakishou.photoexchange.mvp.model.exception.ConnectionError
import kotlinx.coroutines.rx2.await
import net.response.GalleryPhotoInfoResponse

class GetGalleryPhotoInfoRequest(
  private val userId: String,
  private val galleryPhotoIds: String,
  private val apiService: ApiService,
  private val jsonConverter: JsonConverter,
  dispatchersProvider: DispatchersProvider
) : BaseRequest<GalleryPhotoInfoResponse>(dispatchersProvider) {

  override suspend fun execute(): GalleryPhotoInfoResponse {
    val response = try {
      apiService.getGalleryPhotoInfo(userId, galleryPhotoIds).await()
    } catch (error: Exception) {
      throw ConnectionError(error.message)
    }

    val result = handleResponse(jsonConverter, response)
    return when (result) {
      is Either.Value -> result.value
      is Either.Error -> throw result.error
    }
  }
}