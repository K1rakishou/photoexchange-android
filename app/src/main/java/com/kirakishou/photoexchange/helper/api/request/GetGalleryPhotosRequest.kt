package com.kirakishou.photoexchange.helper.api.request

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.rx.operator.OnApiErrorSingle
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvp.model.exception.ApiException
import com.kirakishou.photoexchange.mvp.model.net.response.GalleryPhotosResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Single
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

class GetGalleryPhotosRequest<T>(
    private val galleryPhotoIds: String,
    private val apiService: ApiService,
    private val schedulerProvider: SchedulerProvider,
    private val gson: Gson
) : AbstractRequest<T>() {

    override fun execute(): Single<T> {
        return apiService.getGalleryPhotos(galleryPhotoIds)
            .subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
            .lift(OnApiErrorSingle<GalleryPhotosResponse>(gson, GalleryPhotosResponse::class))
            .map { response ->
                if (ErrorCode.GalleryPhotosErrors.fromInt(response.serverErrorCode!!) is ErrorCode.GalleryPhotosErrors.Ok) {
                    return@map GalleryPhotosResponse.success(response.galleryPhotos)
                } else {
                    return@map GalleryPhotosResponse.fail(ErrorCode.fromInt(ErrorCode.GalleryPhotosErrors::class, response.serverErrorCode!!))
                }
            }
            .onErrorReturn(this::extractError) as Single<T>
    }

    private fun extractError(error: Throwable): GalleryPhotosResponse {
        return when (error) {
            is ApiException -> GalleryPhotosResponse.fail(error.errorCode as ErrorCode.GalleryPhotosErrors)
            is SocketTimeoutException,
            is TimeoutException -> GalleryPhotosResponse.fail(ErrorCode.GalleryPhotosErrors.LocalTimeout())
            else -> GalleryPhotosResponse.fail(ErrorCode.GalleryPhotosErrors.UnknownError())
        }
    }
}