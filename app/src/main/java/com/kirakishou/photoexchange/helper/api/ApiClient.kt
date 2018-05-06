package com.kirakishou.photoexchange.helper.api

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.request.*
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.net.response.*
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

    fun getGalleryPhotoIds(lastId: Long, photosPerPage: Int): Single<GalleryPhotoIdsResponse> {
        return GetGalleryPhotoIdsRequest<GalleryPhotoIdsResponse>(lastId, photosPerPage, apiService, schedulerProvider, gson)
            .execute()
    }

    fun getGalleryPhotos(userId: String, galleryPhotoIds: String): Single<GalleryPhotosResponse> {
        return GetGalleryPhotosRequest<GalleryPhotosResponse>(userId, galleryPhotoIds, apiService, schedulerProvider, gson)
            .execute()
    }

    fun favouritePhoto(userId: String, photoName: String): Single<FavouritePhotoResponse> {
        return FavouritePhotoRequest<FavouritePhotoResponse>(userId, photoName, apiService, schedulerProvider, gson)
            .execute()
    }

    fun reportPhoto(userId: String, photoName: String): Single<ReportPhotoResponse> {
        return ReportPhotoRequest<ReportPhotoResponse>(userId, photoName, apiService, schedulerProvider, gson)
            .execute()
    }

    fun getUserId(): Single<GetUserIdResponse> {
        return GetUserIdRequest<GetUserIdResponse>(apiService, schedulerProvider, gson)
            .execute()
    }
}