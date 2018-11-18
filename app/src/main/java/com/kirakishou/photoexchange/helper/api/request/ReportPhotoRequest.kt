package com.kirakishou.photoexchange.helper.api.request

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.gson.JsonConverter
import com.kirakishou.photoexchange.mvp.model.exception.UnknownException
import com.kirakishou.photoexchange.mvp.model.net.packet.ReportPhotoPacket
import com.kirakishou.photoexchange.mvp.model.net.response.ReportPhotoResponse
import kotlinx.coroutines.rx2.await
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
      throw UnknownException(error)
    }

    val result = handleResponse(jsonConverter, response)
    return when (result) {
      is Either.Value -> result.value
      is Either.Error -> throw result.error
    }
  }
}