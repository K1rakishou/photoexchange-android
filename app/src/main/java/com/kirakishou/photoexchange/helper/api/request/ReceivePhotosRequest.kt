package com.kirakishou.photoexchange.helper.api.request

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.gson.JsonConverter
import com.kirakishou.photoexchange.helper.exception.ConnectionError
import kotlinx.coroutines.rx2.await
import net.response.ReceivedPhotosResponse
import timber.log.Timber

class ReceivePhotosRequest(
  private val userId: String,
  private val photoNames: String,
  private val apiService: ApiService,
  private val jsonConverter: JsonConverter,
  dispatchersProvider: DispatchersProvider
) : BaseRequest<ReceivedPhotosResponse>(dispatchersProvider) {

  override suspend fun execute(): ReceivedPhotosResponse {
    val response = try {
      apiService.receivePhotos(photoNames, userId).await()
    } catch (error: Exception) {
      Timber.e(error)
      throw ConnectionError(error.message)
    }

    val result = handleResponse(jsonConverter, response)
    return when (result) {
      is Either.Value -> result.value
      is Either.Error -> throw result.error
    }
  }
}