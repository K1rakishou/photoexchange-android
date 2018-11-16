package com.kirakishou.photoexchange.helper.api.request

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.rx.operator.OnApiErrorSingle
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.gson.MyGson
import com.kirakishou.photoexchange.mvp.model.exception.GeneralException
import com.kirakishou.photoexchange.mvp.model.net.response.GetUserIdResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Single
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

class GetUserIdRequest<T>(
  private val apiService: ApiService,
  private val schedulerProvider: SchedulerProvider,
  private val gson: MyGson
) : AbstractRequest<T>() {

  override fun execute(): Single<T> {
    return apiService.getUserId()
      .subscribeOn(schedulerProvider.IO())
      .observeOn(schedulerProvider.IO())
      .lift(OnApiErrorSingle<GetUserIdResponse>(gson, GetUserIdResponse::class))
      .map { response ->
        if (ErrorCode.GetUserIdError.fromInt(response.serverErrorCode!!) is ErrorCode.GetUserIdError.Ok) {
          return@map GetUserIdResponse.success(response.userId)
        } else {
          return@map GetUserIdResponse.error(ErrorCode.fromInt(ErrorCode.GetUserIdError::class, response.serverErrorCode!!))
        }
      }
      .onErrorReturn(this::extractError) as Single<T>
  }

  private fun extractError(error: Throwable): GetUserIdResponse {
    return when (error) {
      is GeneralException.ApiException -> GetUserIdResponse.error(error.errorCode as ErrorCode.GetUserIdError)
      is SocketTimeoutException,
      is TimeoutException -> GetUserIdResponse.error(ErrorCode.GetUserIdError.LocalTimeout())
      else -> GetUserIdResponse.error(ErrorCode.GetUserIdError.UnknownError())
    }
  }
}