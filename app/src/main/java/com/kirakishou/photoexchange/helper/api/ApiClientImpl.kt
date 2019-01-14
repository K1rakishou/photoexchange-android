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
    userUuid: String,
    isPublic: Boolean,
    photo: TakenPhoto,
    channel: SendChannel<UploadedPhotosFragmentEvent.PhotoUploadEvent>
  ): UploadPhotosUseCase.UploadPhotoResult {

    val response = UploadPhotoRequest(
      photoFilePath,
      location,
      userUuid,
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
    userUuid: String,
    photoNames: String
  ): List<ReceivedPhotoResponseData> {
    val response = ReceivePhotosRequest(
      userUuid,
      photoNames,
      apiService,
      jsonConverter,
      dispatchersProvider
    ).execute()

    return response.receivedPhotos
  }

  override suspend fun favouritePhoto(
    userUuid: String,
    photoName: String
  ): FavouritePhotoResponseData {
    val response = FavouritePhotoRequest(
      userUuid,
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

  override suspend fun reportPhoto(userUuid: String, photoName: String): Boolean {
    val response = ReportPhotoRequest(
      userUuid,
      photoName,
      apiService,
      jsonConverter,
      dispatchersProvider
    ).execute()

    return response.isReported
  }

  override suspend fun getUserUuid(): String {
    val response = GetUserUuidRequest(
      apiService,
      jsonConverter,
      dispatchersProvider
    ).execute()

    return response.userUuid
  }

  override suspend fun getPageOfUploadedPhotos(
    userUuid: String,
    lastUploadedOn: Long,
    count: Int
  ): List<UploadedPhotoResponseData> {
    val response = GetPageOfUploadedPhotosRequest(
      userUuid,
      lastUploadedOn,
      count,
      apiService,
      jsonConverter,
      dispatchersProvider
    ).execute()

    return response.uploadedPhotos
  }

  override suspend fun getPageOfReceivedPhotos(
    userUuid: String,
    lastUploadedOn: Long,
    count: Int
  ): List<ReceivedPhotoResponseData> {
    val response = GetPageOfReceivedPhotosRequest(
      userUuid,
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
    userUuid: String,
    photoNames: String
  ): List<PhotoAdditionalInfoResponseData> {
    val response = GetPhotosAdditionalInfoRequest(
      userUuid,
      photoNames,
      apiService,
      jsonConverter,
      dispatchersProvider
    ).execute()

    return response.additionalInfoList
  }

  override suspend fun checkAccountExists(userUuid: String): Boolean {
    val response = CheckAccountExistsRequest(
      userUuid,
      apiService,
      jsonConverter,
      dispatchersProvider
    ).execute()

    return response.accountExists
  }

  override suspend fun updateFirebaseToken(userUuid: String, token: String) {
    UpdateFirebaseTokenRequest(
      userUuid,
      token,
      apiService,
      jsonConverter,
      dispatchersProvider
    ).execute()

    //no response data
  }

  override suspend fun getFreshUploadedPhotosCount(userUuid: String, time: Long): Int {
    val response = GetFreshUploadedPhotosCountRequest(
      userUuid,
      time,
      apiService,
      jsonConverter,
      dispatchersProvider
    ).execute()

    return response.freshPhotosCount
  }

  override suspend fun getFreshReceivedPhotosCount(userUuid: String, time: Long): Int {
    val response = GetFreshReceivedPhotosCountRequest(
      userUuid,
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