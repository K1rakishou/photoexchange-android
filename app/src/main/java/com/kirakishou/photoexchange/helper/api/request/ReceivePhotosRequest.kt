package com.kirakishou.photoexchange.helper.api.request

import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.rx.operator.OnApiErrorSingle
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.gson.JsonConverter
import com.kirakishou.photoexchange.mvp.model.exception.ApiException
import com.kirakishou.photoexchange.mvp.model.net.response.ReceivedPhotosResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Single
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

class ReceivePhotosRequest<T>(
  private val userId: String,
  private val photoNames: String,
  private val apiService: ApiService,
  private val schedulerProvider: SchedulerProvider,
  private val jsonConverter: JsonConverter
) : BaseRequest<T>() {

  @Suppress("UNCHECKED_CAST")
  override fun execute(): Single<T> {
    return apiService.receivePhotos(photoNames, userId)
      .subscribeOn(schedulerProvider.IO())
      .observeOn(schedulerProvider.IO())
      .lift(OnApiErrorSingle<ReceivedPhotosResponse>(jsonConverter, ReceivedPhotosResponse::class))
      .map { response ->
        if (ErrorCode.ReceivePhotosErrors.fromInt(response.serverErrorCode!!) is ErrorCode.ReceivePhotosErrors.Ok) {
          return@map ReceivedPhotosResponse.success(response.receivedPhotos)
        } else {
          return@map ReceivedPhotosResponse.error(ErrorCode.fromInt(ErrorCode.ReceivePhotosErrors::class, response.serverErrorCode!!))
        }
      }
      .onErrorReturn(this::extractError) as Single<T>
  }

  private fun extractError(error: Throwable): ReceivedPhotosResponse {
    return when (error) {
      is ApiException -> ReceivedPhotosResponse.error(error.errorCode as ErrorCode.ReceivePhotosErrors)
      is SocketTimeoutException,
      is TimeoutException -> ReceivedPhotosResponse.error(ErrorCode.ReceivePhotosErrors.LocalTimeout())
      else -> ReceivedPhotosResponse.error(ErrorCode.ReceivePhotosErrors.UnknownError())
    }
  }
}