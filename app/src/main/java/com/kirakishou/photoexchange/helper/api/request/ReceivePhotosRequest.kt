package com.kirakishou.photoexchange.helper.api.request

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.gson.JsonConverter
import com.kirakishou.photoexchange.mvp.model.exception.ConnectionError
import kotlinx.coroutines.rx2.await
import net.response.ReceivePhotosResponse

class ReceivePhotosRequest(
  private val userId: String,
  private val photoNames: String,
  private val apiService: ApiService,
  private val jsonConverter: JsonConverter,
  dispatchersProvider: DispatchersProvider
) : BaseRequest<ReceivePhotosResponse>(dispatchersProvider) {

  override suspend fun execute(): ReceivePhotosResponse {
    val response = try {
      apiService.receivePhotos(photoNames, userId).await()
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