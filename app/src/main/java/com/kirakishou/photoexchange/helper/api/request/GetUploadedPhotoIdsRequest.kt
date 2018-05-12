package com.kirakishou.photoexchange.helper.api.request

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.rx.operator.OnApiErrorSingle
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvp.model.exception.ApiException
import com.kirakishou.photoexchange.mvp.model.net.response.GetUploadedPhotoIdsResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Single
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

class GetUploadedPhotoIdsRequest<T>(
    private val userId: String,
    private val lastId: Long,
    private val count: Int,
    private val apiService: ApiService,
    private val schedulerProvider: SchedulerProvider,
    private val gson: Gson
) : AbstractRequest<T>() {

    override fun execute(): Single<T> {
        return apiService.getUploadedPhotoIds(userId, lastId, count)
            .subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
            .lift(OnApiErrorSingle<GetUploadedPhotoIdsResponse>(gson, GetUploadedPhotoIdsResponse::class.java))
            .map { response ->
                if (response.serverErrorCode!! == 0) {
                    return@map GetUploadedPhotoIdsResponse.success(response.uploadedPhotoIds)
                } else {
                    return@map GetUploadedPhotoIdsResponse.fail(ErrorCode.fromInt(ErrorCode.GetUploadedPhotoIdsError::class.java, response.serverErrorCode))
                }
            }
            .onErrorReturn(this::extractError) as Single<T>
    }

    private fun extractError(error: Throwable): GetUploadedPhotoIdsResponse {
        return when (error) {
            is ApiException -> GetUploadedPhotoIdsResponse.fail(error.errorCode)
            is SocketTimeoutException,
            is TimeoutException -> GetUploadedPhotoIdsResponse.fail(ErrorCode.GetUploadedPhotoIdsError.Local.Timeout())
            else -> GetUploadedPhotoIdsResponse.fail(ErrorCode.GetUploadedPhotoIdsError.Remote.UnknownError())
        }
    }
}