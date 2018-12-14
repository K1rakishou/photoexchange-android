package com.kirakishou.photoexchange.helper.api.request

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.exception.ConnectionError
import com.kirakishou.photoexchange.helper.gson.JsonConverter
import kotlinx.coroutines.rx2.await
import net.response.GetPhotosAdditionalInfoResponse
import timber.log.Timber

class GetPhotosAdditionalInfoRequest(
  private val userId: String,
  private val photoNames: String,
  private val apiService: ApiService,
  private val jsonConverter: JsonConverter,
  private val dispatchersProvider: DispatchersProvider
) : BaseRequest<GetPhotosAdditionalInfoResponse>(dispatchersProvider) {

  override suspend fun execute(): GetPhotosAdditionalInfoResponse {
    val response = try {
      apiService.getPhotosAdditionalInfo(userId, photoNames).await()
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