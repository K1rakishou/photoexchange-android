package com.kirakishou.photoexchange.helper.api

import com.kirakishou.photoexchange.helper.api.response.FavouritePhotoResponseData
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
import com.kirakishou.photoexchange.helper.exception.ApiErrorException
import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.mvp.model.photo.TakenPhoto
import kotlinx.coroutines.channels.SendChannel
import net.response.*

interface ApiClient {

  @Throws(ApiErrorException::class)
  suspend fun uploadPhoto(
    photoFilePath: String,
    location: LonLat,
    userId: String,
    isPublic: Boolean,
    photo: TakenPhoto,
    channel: SendChannel<UploadedPhotosFragmentEvent.PhotoUploadEvent>
  ): UploadPhotosUseCase.UploadPhotoResult

  @Throws(ApiErrorException::class)
  suspend fun receivePhotos(userId: String, photoNames: String): List<ReceivedPhotosResponse.ReceivedPhotoResponseData>

  @Throws(ApiErrorException::class)
  suspend fun getPageOfGalleryPhotos(lastUploadedOn: Long, count: Int): List<GalleryPhotosResponse.GalleryPhotoResponseData>

  @Throws(ApiErrorException::class)
  suspend fun getGalleryPhotoInfo(userId: String, galleryPhotoIds: String): List<GalleryPhotoInfoResponse.GalleryPhotosInfoResponseData>

  @Throws(ApiErrorException::class)
  suspend fun favouritePhoto(userId: String, photoName: String): FavouritePhotoResponseData

  @Throws(ApiErrorException::class)
  suspend fun reportPhoto(userId: String, photoName: String): Boolean

  @Throws(ApiErrorException::class)
  suspend fun getUserId(): String

  @Throws(ApiErrorException::class)
  suspend fun getPageOfUploadedPhotos(userId: String, lastUploadedOn: Long, count: Int): List<GetUploadedPhotosResponse.UploadedPhotoResponseData>

  @Throws(ApiErrorException::class)
  suspend fun getPageOfReceivedPhotos(userId: String, lastUploadedOn: Long, count: Int): List<ReceivedPhotosResponse.ReceivedPhotoResponseData>

  @Throws(ApiErrorException::class)
  suspend fun checkAccountExists(userId: String): Boolean

  @Throws(ApiErrorException::class)
  suspend fun updateFirebaseToken(userId: String, token: String)
}