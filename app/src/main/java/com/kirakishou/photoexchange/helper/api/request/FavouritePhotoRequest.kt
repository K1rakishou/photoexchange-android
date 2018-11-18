package com.kirakishou.photoexchange.helper.api.request

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.gson.JsonConverter
import com.kirakishou.photoexchange.mvp.model.exception.ConnectionError
import com.kirakishou.photoexchange.mvp.model.net.packet.FavouritePhotoPacket
import com.kirakishou.photoexchange.mvp.model.net.response.FavouritePhotoResponse
import kotlinx.coroutines.rx2.await

class FavouritePhotoRequest(
  private val userId: String,
  private val photoName: String,
  private val apiService: ApiService,
  private val jsonConverter: JsonConverter,
  dispatchersProvider: DispatchersProvider
) : BaseRequest<FavouritePhotoResponse>(dispatchersProvider) {

  override suspend fun execute(): FavouritePhotoResponse {
    val response = try {
      apiService.favouritePhoto(FavouritePhotoPacket(userId, photoName)).await()
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