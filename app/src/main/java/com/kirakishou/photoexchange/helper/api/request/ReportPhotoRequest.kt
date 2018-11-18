package com.kirakishou.photoexchange.helper.api.request

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.gson.MyGson
import com.kirakishou.photoexchange.mvp.model.exception.ReportPhotoExceptions
import com.kirakishou.photoexchange.mvp.model.net.packet.ReportPhotoPacket
import com.kirakishou.photoexchange.mvp.model.net.response.ReportPhotoResponse
import com.kirakishou.photoexchange.mvp.model.net.response.StatusResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import kotlinx.coroutines.rx2.await
import retrofit2.Response

class ReportPhotoRequest(
  private val userId: String,
  private val photoName: String,
  private val apiService: ApiService,
  private val gson: MyGson,
  dispatchersProvider: DispatchersProvider
) : AbstractRequest<ReportPhotoResponse>(dispatchersProvider) {

  override suspend fun execute(): ReportPhotoResponse {
    val response = try {
      apiService.reportPhoto(ReportPhotoPacket(userId, photoName)).await() as Response<ReportPhotoResponse>
    } catch (error: Exception) {
      throw ReportPhotoExceptions.UnknownException(error)
    }

    val result = handleResponse(response)
    return when (result) {
      is Either.Value -> result.value
      is Either.Error -> throw result.error
    }
  }

  private fun handleResponse(response: Response<ReportPhotoResponse>): Either<ReportPhotoExceptions.ApiErrorException, ReportPhotoResponse> {
    if (!response.isSuccessful) {
      try {
        val responseJson = response.errorBody()!!.string()
        val error = gson.fromJson<ReportPhotoResponse>(responseJson, StatusResponse::class.java)

        //may happen in some rare cases (like when client and server have endpoints with different parameters)
        if (error?.serverErrorCode == null) {
          throw ReportPhotoExceptions.BadServerResponse()
        } else {
          //server returned non-zero status
          throw ReportPhotoExceptions.ApiErrorException(ErrorCode.fromInt(error.serverErrorCode!!))
        }
      } catch (e: Exception) {
        throw e
      }
    }

    return Either.Value(response.body()!!)
  }
}