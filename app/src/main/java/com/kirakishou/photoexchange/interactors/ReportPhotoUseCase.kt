package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Observable

class ReportPhotoUseCase(
    private val apiClient: ApiClient
) {
    private val tag = "ReportPhotoUseCase"

    fun reportPhoto(userId: String, photoName: String): Observable<Boolean> {
        return apiClient.reportPhoto(userId, photoName)
            .map { response ->
                val errorCode = response.errorCode as ErrorCode.ReportPhotoErrors

                return@map when (errorCode) {
                    is ErrorCode.ReportPhotoErrors.Remote.Ok -> response.isReported
                    else -> false
                }
            }
            .toObservable()
    }
}