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

    fun loadNextPageOfGalleryPhotos(userId: String, lastId: Long, photosPerPage: Int): Observable<List<GalleryPhoto>> {
        return apiClient.getGalleryPhotos(userId, lastId, photosPerPage)
            .map { response ->
                val errorCode = response.errorCode

                return@map when (errorCode) {
                    is ErrorCode.GalleryPhotosErrors.Remote.Ok -> GalleryPhotoResponseMapper.toGalleryPhoto(response.galleryPhotos)
                    else -> emptyList()
                }
            }
            .toObservable()
    }
}