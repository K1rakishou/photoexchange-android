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

  override fun receivePhotos(userId: String, photoNames: String): Single<ReceivedPhotosResponse> {
    return ReceivePhotosRequest<ReceivedPhotosResponse>(userId, photoNames, apiService, schedulerProvider, gson)
      .execute()
  }

  override fun getPageOfGalleryPhotos(lastUploadedOn: Long, count: Int): Single<GalleryPhotosResponse> {
    return GetPageOfGalleryPhotosRequest<GalleryPhotosResponse>(lastUploadedOn, count, apiService, schedulerProvider, gson)
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

  override fun getPageOfUploadedPhotos(userId: String, lastUploadedOn: Long, count: Int): Single<GetUploadedPhotosResponse> {
    return GetPageOfUploadedPhotosRequest<GetUploadedPhotosResponse>(userId, lastUploadedOn, count, apiService, schedulerProvider, gson)
      .execute()
  }

  override fun getReceivedPhotos(userId: String, lastUploadedOn: Long, count: Int): Single<GetReceivedPhotosResponse> {
    return GetPageOfReceivedPhotosRequest<GetReceivedPhotosResponse>(userId, lastUploadedOn, count, apiService, schedulerProvider, gson)
      .execute()
  }

  override fun checkAccountExists(userId: String): Single<CheckAccountExistsResponse> {
    return CheckAccountExistsRequest<CheckAccountExistsResponse>(userId, apiService, schedulerProvider, gson)
      .execute()
  }
}