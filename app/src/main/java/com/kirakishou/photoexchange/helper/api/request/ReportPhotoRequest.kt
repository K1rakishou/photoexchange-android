package com.kirakishou.photoexchange.helper.api.request

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.rx.operator.OnApiErrorSingle
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.gson.MyGson
import com.kirakishou.photoexchange.mvp.model.exception.GeneralException
import com.kirakishou.photoexchange.mvp.model.net.packet.ReportPhotoPacket
import com.kirakishou.photoexchange.mvp.model.net.response.ReportPhotoResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Single
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

class ReportPhotoRequest<T>(
    private val userId: String,
    private val photoName: String,
    private val apiService: ApiService,
    private val schedulerProvider: SchedulerProvider,
    private val gson: MyGson
) : AbstractRequest<T>() {

    override fun execute(): Single<T> {
        return apiService.reportPhoto(ReportPhotoPacket(userId, photoName))
            .subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
            .lift(OnApiErrorSingle<ReportPhotoResponse>(gson, ReportPhotoResponse::class))
            .map { response ->
                if (ErrorCode.ReportPhotoErrors.fromInt(response.serverErrorCode!!) is ErrorCode.ReportPhotoErrors.Ok) {
                    return@map ReportPhotoResponse.success(response.isReported)
                } else {
                    return@map ReportPhotoResponse.error(ErrorCode.fromInt(ErrorCode.ReportPhotoErrors::class, response.serverErrorCode!!))
                }
            }
            .onErrorReturn(this::extractError) as Single<T>
    }

    private fun extractError(error: Throwable): ReportPhotoResponse {
        return when (error) {
            is GeneralException.ApiException -> ReportPhotoResponse.error(error.errorCode as ErrorCode.ReportPhotoErrors)
            is SocketTimeoutException,
            is TimeoutException -> ReportPhotoResponse.error(ErrorCode.ReportPhotoErrors.LocalTimeout())
            else -> ReportPhotoResponse.error(ErrorCode.ReportPhotoErrors.UnknownError())
        }
    }
}