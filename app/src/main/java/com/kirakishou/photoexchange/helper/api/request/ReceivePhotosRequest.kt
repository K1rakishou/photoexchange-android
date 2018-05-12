package com.kirakishou.photoexchange.helper.api.request

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.rx.operator.OnApiErrorSingle
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvp.model.exception.ApiException
import com.kirakishou.photoexchange.mvp.model.net.response.ReceivePhotosResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Single
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

class ReceivePhotosRequest<T>(
    private val photoNames: String,
    private val userId: String,
    private val apiService: ApiService,
    private val schedulerProvider: SchedulerProvider,
    private val gson: Gson
) : AbstractRequest<T>() {

    @Suppress("UNCHECKED_CAST")
    override fun execute(): Single<T> {
        return apiService.receivePhotos(photoNames, userId)
            .subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
            .lift(OnApiErrorSingle<ReceivePhotosResponse>(gson, ReceivePhotosResponse::class.java))
            .map { response ->
                if (response.serverErrorCode!! == 0) {
                    return@map ReceivePhotosResponse.success(response.photoAnswers)
                } else {
                    return@map ReceivePhotosResponse.error(ErrorCode.fromInt(ErrorCode.ReceivePhotosErrors::class.java, response.serverErrorCode))
                }
            }
            .onErrorReturn(this::extractError) as Single<T>
    }

    private fun extractError(error: Throwable): ReceivePhotosResponse {
        return when (error) {
            is ApiException -> ReceivePhotosResponse.error(error.errorCode)
            is SocketTimeoutException,
            is TimeoutException -> ReceivePhotosResponse.error(ErrorCode.ReceivePhotosErrors.Local.Timeout())
            else -> ReceivePhotosResponse.error(ErrorCode.ReceivePhotosErrors.Remote.UnknownError())
        }
    }
}