package com.kirakishou.photoexchange.helper.util

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.repository.PhotoAdditionalInfoRepository
import com.kirakishou.photoexchange.mvp.model.photo.PhotoAdditionalInfo

interface PhotoAdditionalInfoUtils {

  suspend fun <T> appendAdditionalPhotoInfo(
    photoAdditionalInfoRepository: PhotoAdditionalInfoRepository,
    apiClient: ApiClient,
    userId: String,
    galleryPhotos: List<T>,
    photoNameSelectorFunc: (T) -> String,
    copyFunc: (T, PhotoAdditionalInfo) -> T
  ): List<T>

}