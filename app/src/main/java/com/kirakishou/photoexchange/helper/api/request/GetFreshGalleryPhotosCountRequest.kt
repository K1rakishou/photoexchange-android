package com.kirakishou.photoexchange.helper.api.request

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.exception.ConnectionError
import com.kirakishou.photoexchange.helper.gson.JsonConverter
import kotlinx.coroutines.rx2.await
import net.response.GetFreshPhotosCountResponse
import timber.log.Timber

class GetFreshGalleryPhotosCountRequest(
  private val time: Long,
  private val apiService: ApiService,
  private val jsonConverter: JsonConverter,
  dispatchersProvider: DispatchersProvider
) : BaseRequest<GetFreshPhotosCountResponse>(dispatchersProvider) {

  override suspend fun execute(): GetFreshPhotosCountResponse {
    val response = try {
      apiService.getFreshGalleryPhotosCount(time).await()
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