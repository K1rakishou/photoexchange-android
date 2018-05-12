package com.kirakishou.photoexchange.helper.api.request

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.rx.operator.OnApiErrorSingle
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvp.model.exception.ApiException
import com.kirakishou.photoexchange.mvp.model.net.response.GetUploadedPhotosResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Single
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

class GetUploadedPhotosRequest<T>(
    private val userId: String,
    private val photoIds: String,
    private val apiService: ApiService,
    private val schedulerProvider: SchedulerProvider,
    private val gson: Gson
) : AbstractRequest<T>() {

    override fun execute(): Single<T> {
        return apiService.getUploadedPhotos(userId, photoIds)
            .subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
            .lift(OnApiErrorSingle<GetUploadedPhotosResponse>(gson, GetUploadedPhotosResponse::class.java))
            .map { response ->
                if (response.serverErrorCode!! == 0) {
                    return@map GetUploadedPhotosResponse.success(response.uploadedPhotos)
                } else {
                    return@map GetUploadedPhotosResponse.fail(ErrorCode.fromInt(ErrorCode.GetUploadedPhotosError::class.java, response.serverErrorCode))
                }
            }
            .onErrorReturn(this::extractError) as Single<T>
    }

    private fun extractError(error: Throwable): GetUploadedPhotosResponse {
        return when (error) {
            is ApiException -> GetUploadedPhotosResponse.fail(error.errorCode)
            is SocketTimeoutException,
            is TimeoutException -> GetUploadedPhotosResponse.fail(ErrorCode.GetUploadedPhotosError.Local.Timeout())
            else -> GetUploadedPhotosResponse.fail(ErrorCode.GetUploadedPhotosError.Remote.UnknownError())
        }
    }
}