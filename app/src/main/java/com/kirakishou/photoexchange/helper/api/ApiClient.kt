package com.kirakishou.photoexchange.helper.api

import com.kirakishou.photoexchange.helper.api.response.FavouritePhotoResponseData
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
import com.kirakishou.photoexchange.helper.exception.ApiErrorException
import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.helper.exception.ConnectionError
import com.kirakishou.photoexchange.mvp.model.photo.TakenPhoto
import kotlinx.coroutines.channels.SendChannel
import net.response.data.GalleryPhotoResponseData
import net.response.data.PhotoAdditionalInfoResponseData
import net.response.data.ReceivedPhotoResponseData
import net.response.data.UploadedPhotoResponseData

interface ApiClient {

  @Throws(ApiErrorException::class, ConnectionError::class)
  suspend fun uploadPhoto(
    photoFilePath: String,
    location: LonLat,
    userId: String,
    isPublic: Boolean,
    photo: TakenPhoto,
    channel: SendChannel<UploadedPhotosFragmentEvent.PhotoUploadEvent>
  ): UploadPhotosUseCase.UploadPhotoResult

  @Throws(ApiErrorException::class, ConnectionError::class)
  suspend fun receivePhotos(userId: String, photoNames: String): List<ReceivedPhotoResponseData>

  @Throws(ApiErrorException::class, ConnectionError::class)
  suspend fun favouritePhoto(userId: String, photoName: String): FavouritePhotoResponseData

  @Throws(ApiErrorException::class, ConnectionError::class)
  suspend fun reportPhoto(userId: String, photoName: String): Boolean

  @Throws(ApiErrorException::class, ConnectionError::class)
  suspend fun getUserUuid(): String

  @Throws(ApiErrorException::class, ConnectionError::class)
  suspend fun getPageOfUploadedPhotos(
    userId: String,
    lastUploadedOn: Long,
    count: Int
  ): List<UploadedPhotoResponseData>

  @Throws(ApiErrorException::class, ConnectionError::class)
  suspend fun getPageOfReceivedPhotos(
    userId: String,
    lastUploadedOn: Long,
    count: Int
  ): List<ReceivedPhotoResponseData>

  @Throws(ApiErrorException::class, ConnectionError::class)
  suspend fun getPageOfGalleryPhotos(
    lastUploadedOn: Long,
    count: Int
  ): List<GalleryPhotoResponseData>

  @Throws(ApiErrorException::class, ConnectionError::class)
  suspend fun getPhotosAdditionalInfo(
    userId: String,
    photoNames: String
  ): List<PhotoAdditionalInfoResponseData>

  @Throws(ApiErrorException::class, ConnectionError::class)
  suspend fun checkAccountExists(userId: String): Boolean

  @Throws(ApiErrorException::class, ConnectionError::class)
  suspend fun updateFirebaseToken(userId: String, token: String)

  @Throws(ApiErrorException::class, ConnectionError::class)
  suspend fun getFreshUploadedPhotosCount(userId: String, time: Long): Int

  @Throws(ApiErrorException::class, ConnectionError::class)
  suspend fun getFreshReceivedPhotosCount(userId: String, time: Long): Int

  @Throws(ApiErrorException::class, ConnectionError::class)
  suspend fun getFreshGalleryPhotosCount(time: Long): Int
}