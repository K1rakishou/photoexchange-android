package com.kirakishou.photoexchange.helper.api

import com.kirakishou.photoexchange.helper.api.request.*
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.gson.JsonConverter
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.interactors.FavouritePhotoUseCase
import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.net.response.*
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import io.reactivex.Single
import kotlinx.coroutines.channels.SendChannel
import javax.inject.Inject

/**
 * Created by kirakishou on 3/17/2018.
 */
open class ApiClientImpl
@Inject constructor(
  private val apiService: ApiService,
  private val jsonConverter: JsonConverter,
  private val schedulerProvider: SchedulerProvider,
  private val dispatchersProvider: DispatchersProvider
) : ApiClient {

  override suspend fun uploadPhoto(
    photoFilePath: String,
    location: LonLat,
    userId: String,
    isPublic: Boolean,
    photo: TakenPhoto,
    channel: SendChannel<UploadedPhotosFragmentEvent.PhotoUploadEvent>
  ): UploadPhotosUseCase.UploadPhotoResult {

    val response = UploadPhotoRequest(
      photoFilePath,
      location,
      userId,
      isPublic,
      photo,
      channel,
      apiService,
      jsonConverter,
      dispatchersProvider
    ).execute()

    return UploadPhotosUseCase.UploadPhotoResult(response.photoId, response.photoName)
  }

  override fun receivePhotos(userId: String, photoNames: String): Single<ReceivedPhotosResponse> {
    return ReceivePhotosRequest<ReceivedPhotosResponse>(userId, photoNames, apiService, schedulerProvider, jsonConverter)
      .execute()
  }

  override suspend fun getPageOfGalleryPhotos(
    lastUploadedOn: Long,
    count: Int
  ): List<GalleryPhotosResponse.GalleryPhotoResponseData> {
    val response = GetPageOfGalleryPhotosRequest(
      lastUploadedOn,
      count,
      apiService,
      jsonConverter,
      dispatchersProvider
    ).execute()

    return response.galleryPhotos
  }

  override suspend fun getGalleryPhotoInfo(
    userId: String,
    galleryPhotoIds: String
  ): List<GalleryPhotoInfoResponse.GalleryPhotosInfoData> {
    val response = GetGalleryPhotoInfoRequest(
      userId,
      galleryPhotoIds,
      apiService,
      jsonConverter,
      dispatchersProvider
    ).execute()

    return response.galleryPhotosInfo
  }

  override suspend fun favouritePhoto(userId: String, photoName: String): FavouritePhotoUseCase.FavouritePhotoResult {
    val response = FavouritePhotoRequest(
      userId,
      photoName,
      apiService,
      jsonConverter,
      dispatchersProvider
    ).execute()

    return FavouritePhotoUseCase.FavouritePhotoResult(response.isFavourited, response.favouritesCount)
  }

  override suspend fun reportPhoto(userId: String, photoName: String): Boolean {
    val response = ReportPhotoRequest(
      userId,
      photoName,
      apiService,
      jsonConverter,
      dispatchersProvider
    ).execute()

    return response.isReported
  }

  override suspend fun getUserId(): String {
    val response = GetUserIdRequest(
      apiService,
      jsonConverter,
      dispatchersProvider
    ).execute()

    return response.userId
  }

  override fun getPageOfUploadedPhotos(userId: String, lastUploadedOn: Long, count: Int): Single<GetUploadedPhotosResponse> {
    return GetPageOfUploadedPhotosRequest<GetUploadedPhotosResponse>(userId, lastUploadedOn, count, apiService, schedulerProvider, jsonConverter)
      .execute()
  }

  override suspend fun getReceivedPhotos(userId: String, lastUploadedOn: Long, count: Int): List<GetReceivedPhotosResponse.ReceivedPhoto> {
    val response = GetPageOfReceivedPhotosRequest(
      userId,
      lastUploadedOn,
      count,
      apiService,
      jsonConverter,
      dispatchersProvider
    ).execute()

    return response.receivedPhotos
  }

  override suspend fun checkAccountExists(userId: String): Boolean {
    val response = CheckAccountExistsRequest(
      userId,
      apiService,
      jsonConverter,
      dispatchersProvider
    ).execute()

    return response.accountExists
  }
}