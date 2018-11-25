package com.kirakishou.photoexchange.helper.api.request

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.gson.JsonConverter
import com.kirakishou.photoexchange.helper.exception.ConnectionError
import kotlinx.coroutines.rx2.await
import net.request.ReportPhotoPacket
import net.response.ReportPhotoResponse
import retrofit2.Response

class ReportPhotoRequest(
  private val userId: String,
  private val photoName: String,
  private val apiService: ApiService,
  private val jsonConverter: JsonConverter,
  dispatchersProvider: DispatchersProvider
) : BaseRequest<ReportPhotoResponse>(dispatchersProvider) {

  override suspend fun execute(): ReportPhotoResponse {
    val response = try {
      apiService.reportPhoto(ReportPhotoPacket(userId, photoName)).await() as Response<ReportPhotoResponse>
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