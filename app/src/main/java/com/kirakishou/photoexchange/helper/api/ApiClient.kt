package com.kirakishou.photoexchange.helper.api

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
  fun favouritePhoto(userId: String, photoName: String): Single<FavouritePhotoResponse>
  fun reportPhoto(userId: String, photoName: String): Single<ReportPhotoResponse>
  fun getUserId(): Single<GetUserIdResponse>
  fun getPageOfUploadedPhotos(userId: String, lastUploadedOn: Long, count: Int): Single<GetUploadedPhotosResponse>
  fun getReceivedPhotoIds(userId: String, lastId: Long, count: Int): Single<GetReceivedPhotoIdsResponse>
  fun getReceivedPhotos(userId: String, photoIds: String): Single<GetReceivedPhotosResponse>
  fun checkAccountExists(userId: String): Single<CheckAccountExistsResponse>
}