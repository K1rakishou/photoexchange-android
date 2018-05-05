package com.kirakishou.photoexchange.helper.api.request

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.rx.operator.OnApiErrorSingle
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvp.model.exception.ApiException
import com.kirakishou.photoexchange.mvp.model.net.response.GalleryPhotoIdsResponse
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
            .lift(OnApiErrorSingle<GalleryPhotoIdsResponse>(gson, GalleryPhotoIdsResponse::class.java))
            .map { response ->
                if (response.serverErrorCode!! == 0) {
                    return@map GalleryPhotoIdsResponse.success(response.galleryPhotoIds)
                } else {
                    return@map GalleryPhotoIdsResponse.error(ErrorCode.fromInt(ErrorCode.GalleryPhotosErrors::class.java, response.serverErrorCode))
                }
            }
            .onErrorReturn(this::extractError) as Single<T>
    }

    private fun extractError(error: Throwable): GalleryPhotoIdsResponse {
        return when (error) {
            is ApiException -> GalleryPhotoIdsResponse.error(error.errorCode as ErrorCode.GalleryPhotosErrors)
            is SocketTimeoutException,
            is TimeoutException -> GalleryPhotoIdsResponse.error(ErrorCode.GalleryPhotosErrors.Local.Timeout())
            else -> GalleryPhotoIdsResponse.error(ErrorCode.GalleryPhotosErrors.Remote.UnknownError())
        }
    }
}