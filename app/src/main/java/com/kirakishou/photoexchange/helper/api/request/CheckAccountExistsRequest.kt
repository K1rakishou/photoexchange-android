package com.kirakishou.photoexchange.helper.api.request

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.gson.JsonConverter
import com.kirakishou.photoexchange.helper.exception.ConnectionError
import kotlinx.coroutines.rx2.await
import net.response.CheckAccountExistsResponse

class CheckAccountExistsRequest(
  private val userId: String,
  private val apiService: ApiService,
  private val jsonConverter: JsonConverter,
  dispatchersProvider: DispatchersProvider
) : BaseRequest<CheckAccountExistsResponse>(dispatchersProvider) {

  override suspend fun execute(): CheckAccountExistsResponse {
    val response = try {
      apiService.checkAccountExists(userId).await()
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