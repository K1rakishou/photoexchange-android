package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Observable
import timber.log.Timber

class ReportPhotoUseCase(
    private val apiClient: ApiClient
) {
    private val TAG = "ReportPhotoUseCase"

    fun reportPhoto(userId: String, photoName: String): Observable<UseCaseResult<Boolean>> {
        return apiClient.reportPhoto(userId, photoName)
            .map { response ->
                val errorCode = response.errorCode as ErrorCode.ReportPhotoErrors

                val result =  when (errorCode) {
                    is ErrorCode.ReportPhotoErrors.Remote.Ok -> UseCaseResult.Result(response.isReported)
                    else -> UseCaseResult.Error(errorCode)
                }

                return@map result as UseCaseResult<Boolean>
            }
            .toObservable()
            .doOnError { Timber.tag(TAG).e(it) }
    }
}