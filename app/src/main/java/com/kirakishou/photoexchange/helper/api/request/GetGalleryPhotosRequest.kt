package com.kirakishou.photoexchange.helper.api.request

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.rx.operator.OnApiErrorSingle
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvp.model.exception.ApiException
import com.kirakishou.photoexchange.mvp.model.net.response.FavouritePhotoResponse
import com.kirakishou.photoexchange.mvp.model.net.response.GalleryPhotosResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Single
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

class GetGalleryPhotosRequest<T>(
    private val lastId: Long,
    private val photosPerPage: Int,
    private val apiService: ApiService,
    private val schedulerProvider: SchedulerProvider,
    private val gson: Gson
) : AbstractRequest<T>() {

    @Suppress("UNCHECKED_CAST")
    override fun execute(): Single<T> {
        return apiService.getGalleryPhotos(lastId, photosPerPage)
            .subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
            .lift(OnApiErrorSingle<GalleryPhotosResponse>(gson, GalleryPhotosResponse::class.java))
            .map { response ->
                if (response.serverErrorCode!! == 0) {
                    return@map GalleryPhotosResponse.success(response.galleryPhotos)
                } else {
                    return@map GalleryPhotosResponse.error(ErrorCode.fromInt(ErrorCode.GalleryPhotosErrors::class.java, response.serverErrorCode))
                }
            }
            .onErrorReturn(this::extractError) as Single<T>
    }

    private fun extractError(error: Throwable): GalleryPhotosResponse {
        return when (error) {
            is ApiException -> GalleryPhotosResponse.error(error.errorCode as ErrorCode.GalleryPhotosErrors)
            is SocketTimeoutException,
            is TimeoutException -> GalleryPhotosResponse.error(ErrorCode.GalleryPhotosErrors.Local.Timeout())
            else -> GalleryPhotosResponse.error(ErrorCode.GalleryPhotosErrors.Remote.UnknownError())
        }
    }
}