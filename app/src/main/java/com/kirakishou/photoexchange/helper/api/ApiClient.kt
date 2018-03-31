package com.kirakishou.photoexchange.helper.api

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.request.UploadPhotoRequest
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvp.model.net.response.UploadPhotoResponse
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import com.kirakishou.photoexchange.service.UploadPhotoServiceCallbacks
import io.reactivex.Single
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * Created by kirakishou on 3/17/2018.
 */
class ApiClient
@Inject constructor(
    private val apiService: ApiService,
    private val gson: Gson,
    private val schedulerProvider: SchedulerProvider
) {

    fun uploadPhoto(photoId: Long, photoFilePath: String, location: LonLat, userId: String,
                    callback: WeakReference<UploadPhotoServiceCallbacks>?): Single<UploadPhotoResponse> {
        return UploadPhotoRequest<UploadPhotoResponse>(photoId, photoFilePath, location, userId, callback, apiService, schedulerProvider, gson)
            .execute()
    }

//    fun findPhotoAnswer(userId: String): Single<PhotoAnswerResponse> {
//        return FindPhotoAnswerRequest(userId, apiService, schedulers, gson)
//            .execute()
//    }
//
//    fun markPhotoAsReceived(photoId: Long, userId: String): Single<StatusResponse> {
//        return MarkPhotoAsReceivedRequest(photoId, userId, apiService, schedulers, gson)
//            .execute()
//    }
//
//    fun getPhotoRecipientsLocations(userId: String, photoIds: String): Single<GetUserLocationResponse> {
//        return GetPhotoNewLocationRequest(userId, photoIds, apiService, schedulers, gson)
//            .execute()
//    }

}