package com.kirakishou.photoexchange.helper.api.request

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.rx.operator.OnApiErrorSingle
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.gson.MyGson
import com.kirakishou.photoexchange.mvp.model.exception.GeneralException
import com.kirakishou.photoexchange.mvp.model.net.response.GetReceivedPhotoIdsResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Single
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

class GetReceivedPhotoIdsRequest<T>(
    private val userId: String,
    private val lastId: Long,
    private val count: Int,
    private val apiService: ApiService,
    private val schedulerProvider: SchedulerProvider,
    private val gson: MyGson
) : AbstractRequest<T>() {

    override fun execute(): Single<T> {
        return apiService.getReceivedPhotoIds(userId, lastId, count)
            .subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
            .lift(OnApiErrorSingle<GetReceivedPhotoIdsResponse>(gson, GetReceivedPhotoIdsResponse::class))
            .map { response ->
                if (ErrorCode.GetReceivedPhotosErrors.fromInt(response.serverErrorCode!!) is ErrorCode.GetReceivedPhotosErrors.Ok) {
                    return@map GetReceivedPhotoIdsResponse.success(response.receivedPhotoIds)
                } else {
                    return@map GetReceivedPhotoIdsResponse.fail(ErrorCode.fromInt(ErrorCode.GetReceivedPhotosErrors::class, response.serverErrorCode!!))
                }
            }
            .onErrorReturn(this::extractError) as Single<T>
    }

    private fun extractError(error: Throwable): GetReceivedPhotoIdsResponse {
        return when (error) {
            is GeneralException.ApiException -> GetReceivedPhotoIdsResponse.fail(error.errorCode)
            is SocketTimeoutException,
            is TimeoutException -> GetReceivedPhotoIdsResponse.fail(ErrorCode.GetReceivedPhotosErrors.LocalTimeout())
            else -> GetReceivedPhotoIdsResponse.fail(ErrorCode.GetReceivedPhotosErrors.UnknownError())
        }
    }
}