package com.kirakishou.photoexchange.helper.api

import com.kirakishou.photoexchange.helper.api.request.*
import com.kirakishou.photoexchange.helper.api.response.FavouritePhotoResponseData
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.gson.JsonConverter
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.mvp.model.photo.TakenPhoto
import kotlinx.coroutines.channels.SendChannel
import net.response.data.GalleryPhotoResponseData
import net.response.data.PhotoAdditionalInfoResponseData
import net.response.data.ReceivedPhotoResponseData
import net.response.data.UploadedPhotoResponseData
import javax.inject.Inject

/**
 * Created by kirakishou on 3/17/2018.
 */
open class ApiClientImpl
@Inject constructor(
  private val apiService: ApiService,
  private val jsonConverter: JsonConverter,
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

    return UploadPhotosUseCase.UploadPhotoResult(
      response.photoId,
      response.photoName,
      response.uploadedOn
    )
  }

  override suspend fun receivePhotos(
    userId: String,
    photoNames: String
  ): List<ReceivedPhotoResponseData> {
    val response = ReceivePhotosRequest(
      userId,
      photoNames,
      apiService,
      jsonConverter,
      dispatchersProvider
    ).execute()

    return response.receivedPhotos
  }

  override suspend fun favouritePhoto(
    userId: String,
    photoName: String
  ): FavouritePhotoResponseData {
    val response = FavouritePhotoRequest(
      userId,
      photoName,
      apiService,
      jsonConverter,
      dispatchersProvider
    ).execute()

    return FavouritePhotoResponseData(
      response.isFavourited,
      response.favouritesCount
    )
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

  override suspend fun getPageOfUploadedPhotos(
    userId: String,
    lastUploadedOn: Long,
    count: Int
  ): List<UploadedPhotoResponseData> {
    val response = GetPageOfUploadedPhotosRequest(
      userId,
      lastUploadedOn,
      count,
      apiService,
      jsonConverter,
      dispatchersProvider
    ).execute()

    return response.uploadedPhotos
  }

  override suspend fun getPageOfReceivedPhotos(
    userId: String,
    lastUploadedOn: Long,
    count: Int
  ): List<ReceivedPhotoResponseData> {
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

  override suspend fun getPageOfGalleryPhotos(
    lastUploadedOn: Long,
    count: Int
  ): List<GalleryPhotoResponseData> {
    val response = GetPageOfGalleryPhotosRequest(
      lastUploadedOn,
      count,
      apiService,
      jsonConverter,
      dispatchersProvider
    ).execute()

    return response.galleryPhotos
  }

  override suspend fun getPhotosAdditionalInfo(
    userId: String,
    photoNames: String
  ): List<PhotoAdditionalInfoResponseData> {
    val response = GetPhotosAdditionalInfoRequest(
      userId,
      photoNames,
      apiService,
      jsonConverter,
      dispatchersProvider
    ).execute()

    return response.additionalInfoList
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

  override suspend fun updateFirebaseToken(userId: String, token: String) {
    UpdateFirebaseTokenRequest(
      userId,
      token,
      apiService,
      jsonConverter,
      dispatchersProvider
    ).execute()

    //no response data
  }

  override suspend fun getFreshUploadedPhotosCount(userId: String, time: Long): Int {
    val response = GetFreshUploadedPhotosCountRequest(
      userId,
      time,
      apiService,
      jsonConverter,
      dispatchersProvider
    ).execute()

    return response.freshPhotosCount
  }

  override suspend fun getFreshReceivedPhotosCount(userId: String, time: Long): Int {
    val response = GetFreshReceivedPhotosCountRequest(
      userId,
      time,
      apiService,
      jsonConverter,
      dispatchersProvider
    ).execute()

    return response.freshPhotosCount
  }

  override suspend fun getFreshGalleryPhotosCount(time: Long): Int {
    val response = GetFreshGalleryPhotosCountRequest(
      time,
      apiService,
      jsonConverter,
      dispatchersProvider
    ).execute()

    return response.freshPhotosCount
  }
}