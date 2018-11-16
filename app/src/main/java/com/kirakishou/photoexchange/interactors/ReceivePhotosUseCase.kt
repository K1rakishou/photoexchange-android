package com.kirakishou.photoexchange.interactors

import android.annotation.SuppressLint
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
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import timber.log.Timber

open class ReceivePhotosUseCase(
  private val database: MyDatabase,
  private val takenPhotosRepository: TakenPhotosRepository,
  private val receivedPhotosRepository: ReceivedPhotosRepository,
  private val uploadedPhotosRepository: UploadedPhotosRepository,
  private val apiClient: ApiClient
) {

  private val TAG = "ReceivePhotosUseCase"

  /**
   * Returns separate observable for every receivedPhoto
   * */
  open fun receivePhotos(photoData: FindPhotosData): Observable<ReceivePhotosServicePresenter.ReceivePhotoEvent> {
    return Observable.create { emitter -> doReceivePhoto(photoData, emitter) }
  }

  private fun doReceivePhoto(
    photoData: FindPhotosData,
    emitter: ObservableEmitter<ReceivePhotosServicePresenter.ReceivePhotoEvent>
  ) {
    try {
      if (photoData.isUserIdEmpty()) {
        throw ReceivePhotosServiceException.CouldNotGetUserId()
      }

      if (photoData.isPhotoNamesEmpty()) {
        throw ReceivePhotosServiceException.PhotoNamesAreEmpty()
      }

      apiClient.receivePhotos(photoData.userId!!, photoData.photoNames)
        .map { response -> handleResponse(response, emitter) }
        .subscribe(
          { receivedPhotos -> handleOnNext(receivedPhotos, emitter) },
          { error -> handleOnError(error, emitter) }
        )
    } catch (error: Throwable) {
      handleOnError(error, emitter)
    }
  }

  private fun handleResponse(
    response: ReceivedPhotosResponse,
    emitter: ObservableEmitter<ReceivePhotosServicePresenter.ReceivePhotoEvent>
  ): List<Pair<ReceivedPhoto, String>> {
    val errorCode = response.errorCode as ErrorCode.ReceivePhotosErrors
    when (errorCode) {
      is ErrorCode.ReceivePhotosErrors.Ok -> {
        return handleSuccessResult(response)
      }
      else -> {
        emitter.onNext(ReceivePhotosServicePresenter.ReceivePhotoEvent.OnKnownError(errorCode))
        return listOf()
      }
    }
  }

  private fun handleOnNext(
    receivedPhotos: List<Pair<ReceivedPhoto, String>>,
    emitter: ObservableEmitter<ReceivePhotosServicePresenter.ReceivePhotoEvent>
  ) {
    receivedPhotos.forEach {
      emitter.onNext(ReceivePhotosServicePresenter.ReceivePhotoEvent.OnReceivedPhoto(it.first, it.second))
    }

    emitter.onComplete()
  }

  private fun handleOnError(
    error: Throwable,
    emitter: ObservableEmitter<ReceivePhotosServicePresenter.ReceivePhotoEvent>
  ) {
    Timber.tag(TAG).e(error)

    val errorCode = tryToFigureOutExceptionErrorCode(error)
    if (errorCode != null) {
      emitter.onNext(ReceivePhotosServicePresenter.ReceivePhotoEvent.OnKnownError(errorCode))
    } else {
      emitter.onNext(ReceivePhotosServicePresenter.ReceivePhotoEvent.OnUnknownError(error))
    }

    emitter.onComplete()
  }

  private fun tryToFigureOutExceptionErrorCode(error: Throwable): ErrorCode.ReceivePhotosErrors? {
    return when (error) {
      is ReceivePhotosServiceException.CouldNotGetUserId -> ErrorCode.ReceivePhotosErrors.LocalCouldNotGetUserId()
      is ReceivePhotosServiceException.PhotoNamesAreEmpty -> ErrorCode.ReceivePhotosErrors.LocalPhotoNamesAreEmpty()
      is ReceivePhotosServiceException.ApiException -> error.remoteErrorCode
      else -> null
    }
  }

  private fun handleSuccessResult(
    response: ReceivedPhotosResponse
  ): MutableList<Pair<ReceivedPhoto, String>> {
    val results = mutableListOf<Pair<ReceivedPhoto, String>>()

    for (receivedPhoto in response.receivedPhotos) {
      val result = tryToUpdatePhotoInTheDatabase(receivedPhoto)

      if (result) {
        val photoAnswer = ReceivedPhotosMapper.FromResponse
          .ReceivedPhotos.toReceivedPhoto(receivedPhoto)
        results += Pair(photoAnswer, photoAnswer.uploadedPhotoName)
      }
    }

    return results
  }

  @SuppressLint("BinaryOperationInTimber")
  private fun tryToUpdatePhotoInTheDatabase(receivedPhoto: ReceivedPhotosResponse.ReceivedPhoto): Boolean {
    return database.transactional {
      if (!receivedPhotosRepository.save(receivedPhoto)) {
        Timber.tag(TAG).w("Could not save photo with " +
          "receivedPhotoName ${receivedPhoto.receivedPhotoName}")
        return@transactional false
      }

      if (!uploadedPhotosRepository.updateReceiverInfo(receivedPhoto.uploadedPhotoName)) {
        Timber.tag(TAG).w("Could not update receiver info with " +
          "uploadedPhotoName ${receivedPhoto.uploadedPhotoName}")
        return@transactional false
      }

      //TODO: is there any photo to delete to begin with? It should probably be deleted after uploading is done
      if (!takenPhotosRepository.deletePhotoByName(receivedPhoto.uploadedPhotoName)) {
        Timber.tag(TAG).w("Could not delete taken photo with " +
          "uploadedPhotoName ${receivedPhoto.uploadedPhotoName}")
        return@transactional false
      }

      return@transactional true
    }
  }
}