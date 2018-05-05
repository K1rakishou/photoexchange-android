package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.api.mapper.GalleryPhotoResponseMapper
import com.kirakishou.photoexchange.mvp.model.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Observable
import timber.log.Timber

class GetGalleryPhotosUseCase(
    private val apiClient: ApiClient
) {
    private val TAG = "GetGalleryPhotosUseCase"

    fun loadNextPageOfGalleryPhotos(userId: String, lastId: Long, photosPerPage: Int): Observable<UseCaseResult<List<GalleryPhoto>>> {
        return apiClient.getGalleryPhotos(userId, lastId, photosPerPage)
            .map { response ->
                val errorCode = response.errorCode

                val result = when (errorCode) {
                    is ErrorCode.GalleryPhotosErrors.Remote.Ok -> UseCaseResult.Result(GalleryPhotoResponseMapper.toGalleryPhoto(response.galleryPhotos))
                    else -> UseCaseResult.Error(errorCode)
                }

                return@map result as UseCaseResult<List<GalleryPhoto>>
            }
            .toObservable()
            .doOnError { Timber.tag(TAG).e(it) }
    }
}