package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.repository.GalleryPhotoRepository
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Observable
import timber.log.Timber

class ReportPhotoUseCase(
    private val apiClient: ApiClient,
    private val galleryPhotoRepository: GalleryPhotoRepository
) {
    private val TAG = "ReportPhotoUseCase"

    fun reportPhoto(userId: String, photoName: String): Observable<Either<ErrorCode, Boolean>> {
        return apiClient.reportPhoto(userId, photoName)
            .map { response ->
                val errorCode = response.errorCode
                if (errorCode !is ErrorCode.ReportPhotoErrors.Ok) {
                    return@map Either.Error(errorCode)
                }

                val galleryPhoto = galleryPhotoRepository.findByPhotoName(photoName)
                if (galleryPhoto != null) {
                    galleryPhotoRepository.reportPhoto(galleryPhoto.galleryPhotoId)
                }

                return@map Either.Value(response.isReported)
            }
            .toObservable()
            .doOnError { Timber.tag(TAG).e(it) }
    }
}