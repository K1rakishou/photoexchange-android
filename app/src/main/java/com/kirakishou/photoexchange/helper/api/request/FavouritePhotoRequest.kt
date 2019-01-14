package com.kirakishou.photoexchange.helper.api.request

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.gson.JsonConverter
import com.kirakishou.photoexchange.helper.exception.ConnectionError
import kotlinx.coroutines.rx2.await
import net.request.FavouritePhotoPacket
import net.response.FavouritePhotoResponse
import timber.log.Timber

class FavouritePhotoRequest(
  private val userUuid: String,
  private val photoName: String,
  private val apiService: ApiService,
  private val jsonConverter: JsonConverter,
  dispatchersProvider: DispatchersProvider
) : BaseRequest<FavouritePhotoResponse>(dispatchersProvider) {

  override suspend fun execute(): FavouritePhotoResponse {
    val response = try {
      apiService.favouritePhoto(FavouritePhotoPacket(userUuid, photoName)).await()
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