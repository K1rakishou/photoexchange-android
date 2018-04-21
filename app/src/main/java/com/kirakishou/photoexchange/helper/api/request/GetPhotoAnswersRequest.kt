package com.kirakishou.photoexchange.helper.api.request

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.rx.operator.OnApiErrorSingle
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvp.model.net.response.PhotoAnswerResponse
import com.kirakishou.photoexchange.mvp.model.net.response.UploadPhotoResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Single

class GetPhotoAnswersRequest<T : PhotoAnswerResponse>(
    private val photoNames: String,
    private val userId: String,
    private val apiService: ApiService,
    private val schedulerProvider: SchedulerProvider,
    private val gson: Gson
): AbstractRequest<T>() {

    @Suppress("UNCHECKED_CAST")
    override fun execute(): Single<T> {
        return apiService.getPhotoAnswers(photoNames, userId)
            .lift(OnApiErrorSingle<PhotoAnswerResponse>(gson, PhotoAnswerResponse::class.java))
            .subscribeOn(schedulerProvider.BG())
            .observeOn(schedulerProvider.BG())
            .onErrorReturn { _ ->
                return@onErrorReturn UploadPhotoResponse.error(ErrorCode.UploadPhotoErrors.UnknownError()) as T
            } as Single<T>
    }
}