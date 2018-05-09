package com.kirakishou.photoexchange.helper.api.request

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.rx.operator.OnApiErrorSingle
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvp.model.exception.ApiException
import com.kirakishou.photoexchange.mvp.model.net.response.GalleryPhotoInfoResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Single
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

class GetGalleryPhotoInfoRequest<T>(
    private val userId: String,
    private val galleryPhotoIds: String,
    private val apiService: ApiService,
    private val schedulerProvider: SchedulerProvider,
    private val gson: Gson
) : AbstractRequest<T>() {

    override fun execute(): Single<T> {
        return apiService.getGalleryPhotoInfo(userId, galleryPhotoIds)
            .subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
            .lift(OnApiErrorSingle<GalleryPhotoInfoResponse>(gson, GalleryPhotoInfoResponse::class.java))
            .map { response ->
                if (response.serverErrorCode!! == 0) {
                    return@map GalleryPhotoInfoResponse.success(response.galleryPhotosInfo)
                } else {
                    return@map GalleryPhotoInfoResponse.fail(ErrorCode.fromInt(ErrorCode.GetGalleryPhotosInfoError::class.java, response.serverErrorCode))
                }
            }
            .onErrorReturn(this::extractError) as Single<T>
    }

    private fun extractError(error: Throwable): GalleryPhotoInfoResponse {
        return when (error) {
            is ApiException -> GalleryPhotoInfoResponse.fail(error.errorCode as ErrorCode.GetGalleryPhotosInfoError)
            is SocketTimeoutException,
            is TimeoutException -> GalleryPhotoInfoResponse.fail(ErrorCode.GetGalleryPhotosInfoError.Local.Timeout())
            else -> GalleryPhotoInfoResponse.fail(ErrorCode.GetGalleryPhotosInfoError.Remote.UnknownError())
        }
    }
}