package com.kirakishou.photoexchange.helper.api

import com.kirakishou.photoexchange.helper.api.request.*
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.gson.MyGson
import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.net.response.*
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import io.reactivex.Single
import javax.inject.Inject

/**
 * Created by kirakishou on 3/17/2018.
 */
open class ApiClientImpl
@Inject constructor(
    private val apiService: ApiService,
    private val gson: MyGson,
    private val schedulerProvider: SchedulerProvider
) : ApiClient {

    override fun uploadPhoto(photoFilePath: String, location: LonLat, userId: String, isPublic: Boolean,
                    callback: UploadPhotosUseCase.PhotoUploadProgressCallback): Single<UploadPhotoResponse> {
        return UploadPhotoRequest<UploadPhotoResponse>(photoFilePath, location, userId, isPublic, callback, apiService, schedulerProvider, gson)
            .execute()
    }

    override fun receivePhotos(photoNames: String, userId: String): Single<ReceivedPhotosResponse> {
        return ReceivePhotosRequest<ReceivedPhotosResponse>(photoNames, userId, apiService, schedulerProvider, gson)
            .execute()
    }

    override fun getGalleryPhotoIds(lastId: Long, photosPerPage: Int): Single<GalleryPhotoIdsResponse> {
        return GetGalleryPhotoIdsRequest<GalleryPhotoIdsResponse>(lastId, photosPerPage, apiService, schedulerProvider, gson)
            .execute()
    }

    override fun getGalleryPhotos(galleryPhotoIds: String): Single<GalleryPhotosResponse> {
        return GetGalleryPhotosRequest<GalleryPhotosResponse>(galleryPhotoIds, apiService, schedulerProvider, gson)
            .execute()
    }

    override fun getGalleryPhotoInfo(userId: String, galleryPhotoIds: String): Single<GalleryPhotoInfoResponse> {
        return GetGalleryPhotoInfoRequest<GalleryPhotoInfoResponse>(userId, galleryPhotoIds, apiService, schedulerProvider, gson)
            .execute()
    }

    override fun favouritePhoto(userId: String, photoName: String): Single<FavouritePhotoResponse> {
        return FavouritePhotoRequest<FavouritePhotoResponse>(userId, photoName, apiService, schedulerProvider, gson)
            .execute()
    }

    override fun reportPhoto(userId: String, photoName: String): Single<ReportPhotoResponse> {
        return ReportPhotoRequest<ReportPhotoResponse>(userId, photoName, apiService, schedulerProvider, gson)
            .execute()
    }

    override fun getUserId(): Single<GetUserIdResponse> {
        return GetUserIdRequest<GetUserIdResponse>(apiService, schedulerProvider, gson)
            .execute()
    }

    override fun getUploadedPhotoIds(userId: String, lastId: Long, count: Int): Single<GetUploadedPhotoIdsResponse> {
        return GetUploadedPhotoIdsRequest<GetUploadedPhotoIdsResponse>(userId, lastId, count, apiService, schedulerProvider, gson)
            .execute()
    }

    override fun getUploadedPhotos(userId: String, photoIds: String): Single<GetUploadedPhotosResponse> {
        return GetUploadedPhotosRequest<GetUploadedPhotosResponse>(userId, photoIds, apiService, schedulerProvider, gson)
            .execute()
    }

    override fun getReceivedPhotoIds(userId: String, lastId: Long, count: Int): Single<GetReceivedPhotoIdsResponse> {
        return GetReceivedPhotoIdsRequest<GetReceivedPhotoIdsResponse>(userId, lastId, count, apiService, schedulerProvider, gson)
            .execute()
    }

    override fun getReceivedPhotos(userId: String, photoIds: String): Single<GetReceivedPhotosResponse> {
        return GetReceivedPhotosRequest<GetReceivedPhotosResponse>(userId, photoIds, apiService, schedulerProvider, gson)
            .execute()
    }
}