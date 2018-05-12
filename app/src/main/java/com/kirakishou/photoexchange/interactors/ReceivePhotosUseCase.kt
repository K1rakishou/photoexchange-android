package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.isFail
import com.kirakishou.photoexchange.helper.database.mapper.ReceivedPhotosMapper
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.mvp.model.FindPhotosData
import com.kirakishou.photoexchange.mvp.model.net.response.ReceivedPhotosResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.service.ReceivePhotosServiceCallbacks
import io.reactivex.Single
import timber.log.Timber
import java.lang.ref.WeakReference

class ReceivePhotosUseCase(
    private val database: MyDatabase,
    private val takenPhotosRepository: TakenPhotosRepository,
    private val settingsRepository: SettingsRepository,
    private val receivedPhotosRepository: ReceivedPhotosRepository,
    private val apiClient: ApiClient
) {

    private val TAG = "ReceivePhotosUseCase"

    fun receivePhotos(data: FindPhotosData, callbacks: WeakReference<ReceivePhotosServiceCallbacks>): Single<Unit> {
        return Single.just(data)
            .flatMap { _data ->
                Timber.tag(TAG).d("Send receivePhotos request")
                apiClient.receivePhotos(_data.photoNames, _data.userId!!)
            }
            .map { response ->
                val errorCode = response.errorCode as ErrorCode.ReceivePhotosErrors
                Timber.tag(TAG).d("Got response, errorCode = $errorCode")

                when (errorCode) {
                    is ErrorCode.ReceivePhotosErrors.Remote.Ok -> handleSuccessResult(response, callbacks)
                    is ErrorCode.ReceivePhotosErrors.Remote.NoPhotosToSendBack -> callbacks.get()?.stopService()
                    else -> callbacks.get()?.onFailed(errorCode)
                }

                Unit
            }
            .doOnError { error ->
                Timber.tag(TAG).e(error)
                callbacks.get()?.onError(error)
            }
    }

    private fun handleSuccessResult(response: ReceivedPhotosResponse, callbacks: WeakReference<ReceivePhotosServiceCallbacks>) {
        for (photoAnswerResponse in response.receivedPhotos) {
            var insertedPhotoAnswerId: Long? = null

            val result = database.transactional {
                insertedPhotoAnswerId = receivedPhotosRepository.insert(photoAnswerResponse)
                if (insertedPhotoAnswerId!!.isFail()) {
                    Timber.tag(TAG).w("Could not save photo with name ${photoAnswerResponse.receivedPhotoName}")
                    return@transactional false
                }

                return@transactional takenPhotosRepository.deletePhotoByName(photoAnswerResponse.uploadedPhotoName)
            }

            val photoAnswer = ReceivedPhotosMapper.toPhotoAnswer(insertedPhotoAnswerId, photoAnswerResponse)

            if (result) {
                val photoId = takenPhotosRepository.findByPhotoIdByName(photoAnswer.uploadedPhotoName)
                callbacks.get()?.onPhotoReceived(photoAnswer, photoId)
            }
        }
    }
}