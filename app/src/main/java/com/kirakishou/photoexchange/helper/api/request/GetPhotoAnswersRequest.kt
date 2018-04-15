package com.kirakishou.photoexchange.helper.api.request

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvp.model.net.response.PhotoAnswerResponse
import com.kirakishou.photoexchange.mvp.model.net.response.StatusResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Single
import retrofit2.Response
import timber.log.Timber

class GetPhotoAnswersRequest<T : PhotoAnswerResponse>(
    private val photoNames: String,
    private val userId: String,
    private val apiService: ApiService,
    private val schedulerProvider: SchedulerProvider,
    private val gson: Gson
): AbstractRequest<T>() {

    @Suppress("UNCHECKED_CAST")
    override fun execute(): Single<T> {
        return Single.fromCallable {
            try {
                val response = apiService.getPhotoAnswers(photoNames, userId).blockingGet() as Response<T>

                return@fromCallable extractResponse(response)
            } catch (error: Throwable) {
                return@fromCallable PhotoAnswerResponse.error(ErrorCode.UNKNOWN_ERROR) as T
            }
        }.subscribeOn(schedulerProvider.BG())
            .observeOn(schedulerProvider.BG())
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractResponse(response: Response<T>): T {
        if (!response.isSuccessful) {
            try {
                val responseJson = response.errorBody()!!.string()
                val error = gson.fromJson<StatusResponse>(responseJson, StatusResponse::class.java)

                //may happen in some rare cases
                return if (error?.serverErrorCode == null) {
                    PhotoAnswerResponse.error(ErrorCode.BAD_SERVER_RESPONSE) as T
                } else {
                    PhotoAnswerResponse.error(ErrorCode.from(error.serverErrorCode)) as T
                }
            } catch (e: Throwable) {
                Timber.e(e)
                return PhotoAnswerResponse.error(ErrorCode.UNKNOWN_ERROR) as T
            }
        }

        return response.body()!!
    }
}