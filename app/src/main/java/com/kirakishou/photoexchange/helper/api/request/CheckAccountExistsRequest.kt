package com.kirakishou.photoexchange.helper.api.request

import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.rx.operator.OnApiErrorSingle
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.gson.MyGson
import com.kirakishou.photoexchange.mvp.model.exception.GeneralException
import com.kirakishou.photoexchange.mvp.model.net.response.CheckAccountExistsResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Single
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

class CheckAccountExistsRequest<T>(
    private val userId: String,
    private val apiService: ApiService,
    private val schedulerProvider: SchedulerProvider,
    private val gson: MyGson
) : AbstractRequest<T>() {

    override fun execute(): Single<T> {
        return apiService.checkAccountExists(userId)
            .subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
            .lift(OnApiErrorSingle<CheckAccountExistsResponse>(gson, CheckAccountExistsResponse::class))
            .map { response ->
                if (ErrorCode.CheckAccountExistsErrors.fromInt(response.serverErrorCode!!) is ErrorCode.CheckAccountExistsErrors.Ok) {
                    return@map CheckAccountExistsResponse.success(response.accountExists)
                } else {
                    return@map CheckAccountExistsResponse.fail(ErrorCode.fromInt(ErrorCode.CheckAccountExistsErrors::class, response.serverErrorCode!!))
                }
            }
            .onErrorReturn(this::extractError) as Single<T>
    }

    private fun extractError(error: Throwable): CheckAccountExistsResponse {
        return when (error) {
            is GeneralException.ApiException -> CheckAccountExistsResponse.fail(error.errorCode)
            is SocketTimeoutException,
            is TimeoutException -> CheckAccountExistsResponse.fail(ErrorCode.CheckAccountExistsErrors.LocalTimeout())
            else -> CheckAccountExistsResponse.fail(ErrorCode.CheckAccountExistsErrors.UnknownError())
        }
    }
}