package com.kirakishou.photoexchange.helper.api

import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.interactors.FavouritePhotoUseCase
import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import kotlinx.coroutines.channels.SendChannel
import net.response.*

interface ApiClient {
  suspend fun uploadPhoto(
    photoFilePath: String,
    location: LonLat,
    userId: String,
    isPublic: Boolean,
    photo: TakenPhoto,
    channel: SendChannel<UploadedPhotosFragmentEvent.PhotoUploadEvent>
  ): UploadPhotosUseCase.UploadPhotoResult

  suspend fun receivePhotos(userId: String, photoNames: String): List<ReceivePhotosResponse.ReceivedPhotoResponseData>
  suspend fun getPageOfGalleryPhotos(lastUploadedOn: Long, count: Int): List<GalleryPhotosResponse.GalleryPhotoResponseData>
  suspend fun getGalleryPhotoInfo(userId: String, galleryPhotoIds: String): List<GalleryPhotoInfoResponse.GalleryPhotosInfoResponseData>
  suspend fun favouritePhoto(userId: String, photoName: String): FavouritePhotoUseCase.FavouritePhotoResult
  suspend fun reportPhoto(userId: String, photoName: String): Boolean
  suspend fun getUserId(): String
  suspend fun getPageOfUploadedPhotos(userId: String, lastUploadedOn: Long, count: Int): List<GetUploadedPhotosResponse.UploadedPhotoResponseData>
  suspend fun getReceivedPhotos(userId: String, lastUploadedOn: Long, count: Int): List<GetReceivedPhotosResponse.ReceivedPhotoResponseData>
  suspend fun checkAccountExists(userId: String): Boolean
}