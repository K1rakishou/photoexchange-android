package com.kirakishou.photoexchange.helper.api

import com.kirakishou.photoexchange.helper.api.response.FavouritePhotoResponseData
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.usecases.UploadPhotosUseCase
import com.kirakishou.photoexchange.helper.exception.ApiErrorException
import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.helper.exception.AttemptToAccessInternetWithMeteredNetworkException
import com.kirakishou.photoexchange.helper.exception.AttemptToLoadImagesWithMeteredNetworkException
import com.kirakishou.photoexchange.helper.exception.ConnectionError
import com.kirakishou.photoexchange.mvrx.model.photo.TakenPhoto
import kotlinx.coroutines.channels.SendChannel
import net.response.data.GalleryPhotoResponseData
import net.response.data.PhotoAdditionalInfoResponseData
import net.response.data.ReceivedPhotoResponseData
import net.response.data.UploadedPhotoResponseData

interface ApiClient {

  @Throws(ApiErrorException::class, ConnectionError::class, AttemptToLoadImagesWithMeteredNetworkException::class)
  suspend fun uploadPhoto(
    photoFilePath: String,
    location: LonLat,
    userUuid: String,
    isPublic: Boolean,
    photo: TakenPhoto,
    channel: SendChannel<UploadedPhotosFragmentEvent.PhotoUploadEvent>
  ): UploadPhotosUseCase.UploadPhotoResult

  @Throws(ApiErrorException::class, ConnectionError::class, AttemptToAccessInternetWithMeteredNetworkException::class)
  suspend fun receivePhotos(userUuid: String, photoNames: String): List<ReceivedPhotoResponseData>

  @Throws(ApiErrorException::class, ConnectionError::class, AttemptToAccessInternetWithMeteredNetworkException::class)
  suspend fun favouritePhoto(userUuid: String, photoName: String): FavouritePhotoResponseData

  @Throws(ApiErrorException::class, ConnectionError::class, AttemptToAccessInternetWithMeteredNetworkException::class)
  suspend fun reportPhoto(userUuid: String, photoName: String): Boolean

  @Throws(ApiErrorException::class, ConnectionError::class, AttemptToAccessInternetWithMeteredNetworkException::class)
  suspend fun getUserUuid(): String

  @Throws(ApiErrorException::class, ConnectionError::class, AttemptToAccessInternetWithMeteredNetworkException::class)
  suspend fun getPageOfUploadedPhotos(
    userUuid: String,
    lastUploadedOn: Long?,
    count: Int
  ): List<UploadedPhotoResponseData>

  @Throws(ApiErrorException::class, ConnectionError::class, AttemptToAccessInternetWithMeteredNetworkException::class)
  suspend fun getPageOfReceivedPhotos(
    userUuid: String,
    lastUploadedOn: Long?,
    count: Int
  ): List<ReceivedPhotoResponseData>

  @Throws(ApiErrorException::class, ConnectionError::class, AttemptToAccessInternetWithMeteredNetworkException::class)
  suspend fun getPageOfGalleryPhotos(
    lastUploadedOn: Long?,
    count: Int
  ): List<GalleryPhotoResponseData>

  @Throws(ApiErrorException::class, ConnectionError::class, AttemptToAccessInternetWithMeteredNetworkException::class)
  suspend fun getPhotosAdditionalInfo(
    userUuid: String,
    photoNames: String
  ): List<PhotoAdditionalInfoResponseData>

  @Throws(ApiErrorException::class, ConnectionError::class, AttemptToAccessInternetWithMeteredNetworkException::class)
  suspend fun checkAccountExists(userUuid: String): Boolean

  @Throws(ApiErrorException::class, ConnectionError::class, AttemptToAccessInternetWithMeteredNetworkException::class)
  suspend fun updateFirebaseToken(userUuid: String, token: String)

  @Throws(ApiErrorException::class, ConnectionError::class, AttemptToAccessInternetWithMeteredNetworkException::class)
  suspend fun getFreshUploadedPhotosCount(userUuid: String, time: Long): Int

  @Throws(ApiErrorException::class, ConnectionError::class, AttemptToAccessInternetWithMeteredNetworkException::class)
  suspend fun getFreshReceivedPhotosCount(userUuid: String, time: Long): Int

  @Throws(ApiErrorException::class, ConnectionError::class, AttemptToAccessInternetWithMeteredNetworkException::class)
  suspend fun getFreshGalleryPhotosCount(time: Long): Int
}