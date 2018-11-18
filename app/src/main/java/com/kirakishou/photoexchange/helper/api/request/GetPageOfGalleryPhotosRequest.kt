package com.kirakishou.photoexchange.helper.api.request

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.gson.JsonConverter
import com.kirakishou.photoexchange.mvp.model.exception.ConnectionError
import com.kirakishou.photoexchange.mvp.model.net.response.GalleryPhotosResponse
import kotlinx.coroutines.rx2.await

class GetPageOfGalleryPhotosRequest(
  private val lastUploadedOn: Long,
  private val count: Int,
  private val apiService: ApiService,
  private val jsonConverter: JsonConverter,
  private val dispatchersProvider: DispatchersProvider
) : BaseRequest<GalleryPhotosResponse>(dispatchersProvider) {

  override suspend fun execute(): GalleryPhotosResponse {
    val response = try {
      apiService.getPageOfGalleryPhotos(lastUploadedOn, count).await()
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