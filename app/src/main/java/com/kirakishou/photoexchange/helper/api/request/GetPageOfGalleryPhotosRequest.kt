package com.kirakishou.photoexchange.helper.api.request

import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.rx.operator.OnApiErrorSingle
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.gson.JsonConverter
import com.kirakishou.photoexchange.mvp.model.exception.ApiException
import com.kirakishou.photoexchange.mvp.model.net.response.GalleryPhotosResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Single
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

class GetPageOfGalleryPhotosRequest<T>(
  private val lastUploadedOn: Long,
  private val count: Int,
  private val apiService: ApiService,
  private val schedulerProvider: SchedulerProvider,
  private val jsonConverter: JsonConverter
) : BaseRequest<T>() {

  override fun execute(): Single<T> {
    return apiService.getPageOfGalleryPhotos(lastUploadedOn, count)
      .subscribeOn(schedulerProvider.IO())
      .observeOn(schedulerProvider.IO())
      .lift(OnApiErrorSingle<GalleryPhotosResponse>(jsonConverter, GalleryPhotosResponse::class))
      .map { response ->
        if (ErrorCode.GetGalleryPhotosErrors.fromInt(response.serverErrorCode!!) is ErrorCode.GetGalleryPhotosErrors.Ok) {
          return@map GalleryPhotosResponse.success(response.galleryPhotos)
        } else {
          return@map GalleryPhotosResponse.fail(ErrorCode.fromInt(ErrorCode.GetGalleryPhotosErrors::class, response.serverErrorCode!!))
        }
      }
      .onErrorReturn(this::extractError) as Single<T>
  }

  private fun extractError(error: Throwable): GalleryPhotosResponse {
    return when (error) {
      is ApiException -> GalleryPhotosResponse.fail(error.errorCode as ErrorCode.GetGalleryPhotosErrors)
      is SocketTimeoutException,
      is TimeoutException -> GalleryPhotosResponse.fail(ErrorCode.GetGalleryPhotosErrors.LocalTimeout())
      else -> GalleryPhotosResponse.fail(ErrorCode.GetGalleryPhotosErrors.UnknownError())
    }
  }
}