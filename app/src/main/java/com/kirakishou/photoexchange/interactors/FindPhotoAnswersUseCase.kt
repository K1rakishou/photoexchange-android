package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository

class FindPhotoAnswersUseCase(
    private val database: MyDatabase,
    private val myPhotosRepository: PhotosRepository,
    private val apiClient: ApiClient
) {
}