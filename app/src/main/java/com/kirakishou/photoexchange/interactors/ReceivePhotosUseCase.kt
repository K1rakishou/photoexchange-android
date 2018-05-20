package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.isFail
import com.kirakishou.photoexchange.helper.database.mapper.ReceivedPhotosMapper
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
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
    private val uploadedPhotosRepository: UploadedPhotosRepository,
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
                    is ErrorCode.ReceivePhotosErrors.Ok -> handleSuccessResult(response, callbacks)
                    is ErrorCode.ReceivePhotosErrors.NoPhotosToSendBack -> callbacks.get()?.stopService()
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
        for (receivedPhoto in response.receivedPhotos) {
            val result = database.transactional {
                if (!receivedPhotosRepository.save(receivedPhoto)) {
                    Timber.tag(TAG).w("Could not save photo with name ${receivedPhoto.receivedPhotoName}")
                    return@transactional false
                }

                if (!uploadedPhotosRepository.updateReceiverInfo(receivedPhoto.uploadedPhotoName, receivedPhoto.lon, receivedPhoto.lat)) {
                    Timber.tag(TAG).w("Could not update receiver name for photo ${receivedPhoto.uploadedPhotoName}")
                    return@transactional false
                }

                //TODO: probably should move this out of the transaction
                return@transactional takenPhotosRepository.deletePhotoByName(receivedPhoto.uploadedPhotoName)
            }

            val photoAnswer = ReceivedPhotosMapper.FromResponse.ReceivedPhotos.toReceivedPhoto(receivedPhoto)

            if (result) {
                val takenPhotoId = uploadedPhotosRepository.findByPhotoIdByName(photoAnswer.uploadedPhotoName)
                callbacks.get()?.onPhotoReceived(photoAnswer, takenPhotoId)
            }
        }
    }
}