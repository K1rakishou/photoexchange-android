package com.kirakishou.photoexchange.helper.api.request

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.rx.operator.OnApiErrorSingle
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvvm.model.net.response.PhotoAnswerResponse
import io.reactivex.Single

/**
 * Created by kirakishou on 11/12/2017.
 */
class FindPhotoAnswerRequest(
        private val userId: String,
        private val apiService: ApiService,
        private val schedulers: SchedulerProvider,
        private val gson: Gson
) : AbstractRequest<Single<PhotoAnswerResponse>>() {

    override fun build(): Single<PhotoAnswerResponse> {
        return apiService.findPhotoAnswer(userId)
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .lift(OnApiErrorSingle(gson))
                .onErrorResumeNext { error -> convertExceptionToErrorCode(error) }
    }
}