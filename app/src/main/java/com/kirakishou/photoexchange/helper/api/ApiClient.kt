package com.kirakishou.photoexchange.helper.api

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.request.GetGalleryPhotosRequest
import com.kirakishou.photoexchange.helper.api.request.GetPhotoAnswersRequest
import com.kirakishou.photoexchange.helper.api.request.UploadPhotoRequest
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.net.response.GalleryPhotosResponse
import com.kirakishou.photoexchange.mvp.model.net.response.PhotoAnswerResponse
import com.kirakishou.photoexchange.mvp.model.net.response.UploadPhotoResponse
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import io.reactivex.Single
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

    fun uploadPhoto(photoFilePath: String, location: LonLat, userId: String, isPublic: Boolean,
                    callback: UploadPhotosUseCase.PhotoUploadProgressCallback): Single<UploadPhotoResponse> {
        return UploadPhotoRequest<UploadPhotoResponse>(photoFilePath, location, userId, isPublic, callback, apiService, schedulerProvider, gson)
            .execute()
    }

    fun getPhotoAnswers(photoNames: String, userId: String): Single<PhotoAnswerResponse> {
        return GetPhotoAnswersRequest<PhotoAnswerResponse>(photoNames, userId, apiService, schedulerProvider, gson)
            .execute()
    }

    fun getGalleryPhotos(lastId: Long, photosPerPage: Int): Single<GalleryPhotosResponse> {
        return GetGalleryPhotosRequest<GalleryPhotosResponse>(lastId, photosPerPage, apiService, schedulerProvider, gson)
            .execute()
    }
}