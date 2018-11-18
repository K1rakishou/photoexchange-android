package com.kirakishou.photoexchange.helper.api.request

import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.rx.operator.OnApiErrorSingle
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.gson.JsonConverter
import com.kirakishou.photoexchange.mvp.model.exception.ApiException
import com.kirakishou.photoexchange.mvp.model.net.response.GetUploadedPhotosResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Single
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

class GetPageOfUploadedPhotosRequest<T>(
  private val userId: String,
  private val lastUploadedOn: Long,
  private val count: Int,
  private val apiService: ApiService,
  private val schedulerProvider: SchedulerProvider,
  private val jsonConverter: JsonConverter
) : BaseRequest<T>() {

  override fun execute(): Single<T> {
    return apiService.getPageOfUploadedPhotos(userId, lastUploadedOn, count)
      .subscribeOn(schedulerProvider.IO())
      .observeOn(schedulerProvider.IO())
      .lift(OnApiErrorSingle<GetUploadedPhotosResponse>(jsonConverter, GetUploadedPhotosResponse::class))
      .map { response ->
        if (ErrorCode.GetUploadedPhotosErrors.fromInt(response.serverErrorCode!!) is ErrorCode.GetUploadedPhotosErrors.Ok) {
          return@map GetUploadedPhotosResponse.success(response.uploadedPhotos)
        } else {
          return@map GetUploadedPhotosResponse.fail(ErrorCode.fromInt(ErrorCode.GetUploadedPhotosErrors::class, response.serverErrorCode!!))
        }
      }
      .onErrorReturn(this::extractError) as Single<T>
  }

  private fun extractError(error: Throwable): GetUploadedPhotosResponse {
    return when (error) {
      is ApiException -> GetUploadedPhotosResponse.fail(error.errorCode)
      is SocketTimeoutException,
      is TimeoutException -> GetUploadedPhotosResponse.fail(ErrorCode.GetUploadedPhotosErrors.LocalTimeout())
      else -> GetUploadedPhotosResponse.fail(ErrorCode.GetUploadedPhotosErrors.UnknownError())
    }
  }
}