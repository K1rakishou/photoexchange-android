package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.api.mapper.PhotoAnswerResponseMapper
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.isFail
import com.kirakishou.photoexchange.helper.database.repository.PhotoAnswerRepository
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.mvp.model.FindPhotosData
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.net.response.PhotoAnswerResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.service.FindPhotoAnswerServiceCallbacks
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber
import java.lang.ref.WeakReference

class FindPhotoAnswersUseCase(
    private val database: MyDatabase,
    private val myPhotosRepository: PhotosRepository,
    private val settingsRepository: SettingsRepository,
    private val photoAnswerRepository: PhotoAnswerRepository,
    private val apiClient: ApiClient
) {

    private val tag = "FindPhotoAnswersUseCase"

    fun getPhotoAnswers(data: FindPhotosData, callbacks: WeakReference<FindPhotoAnswerServiceCallbacks>): Maybe<Unit> {
        return Maybe.fromCallable {
            try {
                val userId = data.userId!!
                val photoNames = data.photoNames

                val response = apiClient.getPhotoAnswers(photoNames, userId).blockingGet()
                val errorCode = response.errorCode as ErrorCode.FindPhotoAnswerErrors

                when (errorCode) {
                    is ErrorCode.FindPhotoAnswerErrors.Remote.Ok -> handleSuccessResult(response, callbacks)
                    else -> callbacks.get()?.onFailed(errorCode)
                }

            } catch (error: Exception) {
                Timber.e(error)
                callbacks.get()?.onError(error)
            }
        }
    }

    private fun handleSuccessResult(response: PhotoAnswerResponse, callbacks: WeakReference<FindPhotoAnswerServiceCallbacks>) {
        for (photoAnswerResponse in response.photoAnswers) {
            var insertedPhotoAnswerId: Long? = null

            val result = database.transactional {
                insertedPhotoAnswerId = photoAnswerRepository.insert(photoAnswerResponse)
                if (insertedPhotoAnswerId!!.isFail()) {
                    Timber.tag(tag).w("Could not save photo with name ${photoAnswerResponse.photoAnswerName}")
                    return@transactional false
                }

                return@transactional myPhotosRepository.updatePhotoState(photoAnswerResponse.uploadedPhotoName,
                    PhotoState.PHOTO_UPLOADED_ANSWER_RECEIVED)
            }

            val photoAnswer = PhotoAnswerResponseMapper.toPhotoAnswer(insertedPhotoAnswerId, photoAnswerResponse)

            if (result) {
                val photoId = myPhotosRepository.findByPhotoIdByName(photoAnswer.uploadedPhotoName)
                callbacks.get()?.onPhotoReceived(photoAnswer, photoId)
            }
        }
    }
}