package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.mapper.ReceivedPhotosMapper
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.mvp.model.FindPhotosData
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.exception.ReceivePhotosServiceException
import com.kirakishou.photoexchange.mvp.model.net.response.ReceivedPhotosResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.service.ReceivePhotosServicePresenter
import io.reactivex.ObservableEmitter
import timber.log.Timber

class ReceivePhotosUseCase(
    private val database: MyDatabase,
    private val takenPhotosRepository: TakenPhotosRepository,
    private val receivedPhotosRepository: ReceivedPhotosRepository,
    private val uploadedPhotosRepository: UploadedPhotosRepository,
    private val apiClient: ApiClient
) {

    private val TAG = "ReceivePhotosUseCase"

    fun receivePhotos(data: FindPhotosData, emitter: ObservableEmitter<ReceivePhotosServicePresenter.ReceivePhotoEvent>) {
        apiClient.receivePhotos(data.photoNames, data.userId!!)
            .map { response ->
                val errorCode = response.errorCode as ErrorCode.ReceivePhotosErrors
                Timber.tag(TAG).d("Got response, errorCode = $errorCode")

                when (errorCode) {
                    is ErrorCode.ReceivePhotosErrors.Ok -> {
                        return@map handleSuccessResult(response)
                    }
                    is ErrorCode.ReceivePhotosErrors.NotEnoughPhotosOnServer -> {
                        throw ReceivePhotosServiceException.NoPhotosToSendBack()
                    }
                    else -> {
                        throw ReceivePhotosServiceException.OnKnownError(errorCode)
                    }
                }
            }
            .subscribe({ receivedPhotos ->
                receivedPhotos.forEach {
                    emitter.onNext(ReceivePhotosServicePresenter.ReceivePhotoEvent.OnReceivedPhoto(it.first, it.second))
                }

                emitter.onComplete()
            }, { error ->
                Timber.tag(TAG).e(error)
                emitter.onError(error)
            })
    }

    private fun handleSuccessResult(response: ReceivedPhotosResponse): MutableList<Pair<ReceivedPhoto, String>> {
        val results = mutableListOf<Pair<ReceivedPhoto, String>>()

        for (receivedPhoto in response.receivedPhotos) {
            val result = database.transactional {
                if (!receivedPhotosRepository.save(receivedPhoto)) {
                    Timber.tag(TAG).w("Could not save photo with name ${receivedPhoto.receivedPhotoName}")
                    return@transactional false
                }

                uploadedPhotosRepository.updateReceiverInfo(receivedPhoto.uploadedPhotoName)
                return@transactional takenPhotosRepository.deletePhotoByName(receivedPhoto.uploadedPhotoName)
            }

            if (result) {
                val photoAnswer = ReceivedPhotosMapper.FromResponse.ReceivedPhotos.toReceivedPhoto(receivedPhoto)
                results += Pair(photoAnswer, photoAnswer.uploadedPhotoName)
            }
        }

        return results
    }
}