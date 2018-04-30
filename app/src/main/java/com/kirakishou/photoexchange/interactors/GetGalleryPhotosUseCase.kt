package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.api.mapper.GalleryPhotoResponseMapper
import com.kirakishou.photoexchange.mvp.model.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Observable

class GetGalleryPhotosUseCase(
    private val apiClient: ApiClient
) {
    private val tag = "GetGalleryPhotosUseCase"

    fun loadNextPageOfGalleryPhotos(lastId: Long): Observable<List<GalleryPhoto>> {
        return Observable.fromCallable {
            val response = apiClient.getGalleryPhotos(lastId).blockingGet()
            val errorCode = response.errorCode

            return@fromCallable when (errorCode) {
                is ErrorCode.GalleryPhotosErrors.Remote.Ok -> GalleryPhotoResponseMapper.toGalleryPhoto(response.galleryPhotos)
                else -> emptyList()
            }
        }
    }
}