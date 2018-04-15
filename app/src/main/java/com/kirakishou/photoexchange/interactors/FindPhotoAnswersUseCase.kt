package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.PhotoAnswerRepository
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import timber.log.Timber

class FindPhotoAnswersUseCase(
    private val database: MyDatabase,
    private val myPhotosRepository: PhotosRepository,
    private val settingsRepository: SettingsRepository,
    private val photoAnswerRepository: PhotoAnswerRepository,
    private val apiClient: ApiClient
) {

    fun getPhotoAnswers(): Boolean {
        val userId = settingsRepository.getUserId()!!
        val uploadedPhotos = myPhotosRepository.findAllByState(PhotoState.PHOTO_UPLOADED)
        if (uploadedPhotos.isEmpty()) {
            Timber.d("No uploaded photos found")
            return false
        }

        val photoNames = uploadedPhotos
            .map { it.photoName!! }
            .joinToString(",")

        try {
            val response = apiClient.getPhotoAnswers(photoNames, userId).blockingGet()
            when (ErrorCode.from(response.serverErrorCode)) {

                ErrorCode.OK -> TODO()
                ErrorCode.BAD_REQUEST -> TODO()
                ErrorCode.REPOSITORY_ERROR -> TODO()
                ErrorCode.DISK_ERROR -> TODO()
                ErrorCode.NO_PHOTOS_TO_SEND_BACK -> TODO()
                ErrorCode.BAD_PHOTO_ID -> TODO()
                ErrorCode.UPLOAD_MORE_PHOTOS -> TODO()
                ErrorCode.NOT_FOUND -> TODO()

                ErrorCode.BAD_SERVER_RESPONSE -> TODO()
                ErrorCode.BAD_ERROR_CODE -> TODO()
                ErrorCode.UNKNOWN_ERROR -> TODO()
            }

            val repoResults = arrayListOf<Boolean>()

            for (photoAnswer in response.photoAnswers) {
                val result = database.transactional {
                    if (!photoAnswerRepository.insert(photoAnswer)) {
                        Timber.w("Could not insert photo with name ${photoAnswer.photoAnswerName}")
                        return@transactional false
                    }

                    return@transactional myPhotosRepository.updatePhotoState(photoAnswer.photoAnswerName,
                        PhotoState.PHOTO_UPLOADED_ANSWER_RECEIVED)
                }

                if (result) {
                    //TODO: send to the server that photo was received successfully
                }

                repoResults += result
            }

            return repoResults.none { !it }
        } catch (error: Throwable) {
            Timber.e(error)
            return false
        }
    }
}