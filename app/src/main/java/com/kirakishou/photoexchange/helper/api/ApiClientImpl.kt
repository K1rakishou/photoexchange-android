package com.kirakishou.photoexchange.helper.api

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.request.FindPhotoAnswerRequest
import com.kirakishou.photoexchange.helper.api.request.MarkPhotoAsReceivedRequest
import com.kirakishou.photoexchange.helper.api.request.UploadPhotoRequest
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mwvm.model.dto.PhotoToBeUploaded
import com.kirakishou.photoexchange.mwvm.model.net.response.PhotoAnswerResponse
import com.kirakishou.photoexchange.mwvm.model.net.response.StatusResponse
import com.kirakishou.photoexchange.mwvm.model.net.response.UploadPhotoResponse
import io.reactivex.Single
import javax.inject.Inject


/**
 * Created by kirakishou on 7/22/2017.
 */

class ApiClientImpl
@Inject constructor(
        protected val apiService: ApiService,
        protected val gson: Gson,
        protected val schedulers: SchedulerProvider
) : ApiClient {

    override fun uploadPhoto(info: PhotoToBeUploaded): Single<UploadPhotoResponse> {
        return UploadPhotoRequest(info, apiService, schedulers, gson)
                .build()
    }

    override fun findPhotoAnswer(userId: String): Single<PhotoAnswerResponse> {
        return FindPhotoAnswerRequest(userId, apiService, schedulers, gson)
                .build()
    }

    override fun markPhotoAsReceived(photoId: Long, userId: String): Single<StatusResponse> {
        return MarkPhotoAsReceivedRequest(photoId, userId, apiService, schedulers, gson)
                .build()
    }
}




































