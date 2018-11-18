package com.kirakishou.photoexchange.helper.api

import com.kirakishou.photoexchange.interactors.FavouritePhotoUseCase
import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.net.response.*
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import io.reactivex.Single

interface ApiClient {
  fun uploadPhoto(photoFilePath: String, location: LonLat, userId: String, isPublic: Boolean,
                  callback: UploadPhotosUseCase.PhotoUploadProgressCallback): Single<UploadPhotoResponse>

  fun receivePhotos(userId: String, photoNames: String): Single<ReceivedPhotosResponse>
  fun getPageOfGalleryPhotos(lastUploadedOn: Long, count: Int): Single<GalleryPhotosResponse>
  fun getGalleryPhotoInfo(userId: String, galleryPhotoIds: String): Single<GalleryPhotoInfoResponse>
  suspend fun favouritePhoto(userId: String, photoName: String): FavouritePhotoUseCase.FavouritePhotoResult
  suspend fun reportPhoto(userId: String, photoName: String): Boolean
  fun getUserId(): Single<GetUserIdResponse>
  fun getPageOfUploadedPhotos(userId: String, lastUploadedOn: Long, count: Int): Single<GetUploadedPhotosResponse>
  fun getReceivedPhotos(userId: String, lastUploadedOn: Long, count: Int): Single<GetReceivedPhotosResponse>
  fun checkAccountExists(userId: String): Single<CheckAccountExistsResponse>
}