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
import timber.log.Timber

class ReportPhotoRequest(
  private val userUuid: String,
  private val photoName: String,
  private val apiService: ApiService,
  private val jsonConverter: JsonConverter,
  dispatchersProvider: DispatchersProvider
) : BaseRequest<ReportPhotoResponse>(dispatchersProvider) {

  override suspend fun execute(): ReportPhotoResponse {
    val response = try {
      apiService.reportPhoto(ReportPhotoPacket(userUuid, photoName)).await() as Response<ReportPhotoResponse>
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