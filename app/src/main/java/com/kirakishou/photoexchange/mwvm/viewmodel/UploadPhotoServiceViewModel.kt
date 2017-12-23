package com.kirakishou.photoexchange.mwvm.viewmodel

import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.helper.CompositeJob
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.rx.RxUtils
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.util.FileUtils
import com.kirakishou.photoexchange.mwvm.model.dto.PhotoToBeUploaded
import com.kirakishou.photoexchange.mwvm.model.exception.ApiException
import com.kirakishou.photoexchange.mwvm.model.other.PhotoUploadingStatus
import com.kirakishou.photoexchange.mwvm.model.other.ServerErrorCode
import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto
import com.kirakishou.photoexchange.mwvm.wires.errors.UploadPhotoServiceErrors
import com.kirakishou.photoexchange.mwvm.wires.inputs.UploadPhotoServiceInputs
import com.kirakishou.photoexchange.mwvm.wires.outputs.UploadPhotoServiceOutputs
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.rx2.await
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Created by kirakishou on 11/4/2017.
 */


class UploadPhotoServiceViewModel(
        private val apiClient: ApiClient,
        private val schedulers: SchedulerProvider,
        private val takenPhotosRepo: TakenPhotosRepository
) : UploadPhotoServiceInputs,
        UploadPhotoServiceOutputs,
        UploadPhotoServiceErrors {

    val inputs: UploadPhotoServiceInputs = this
    val outputs: UploadPhotoServiceOutputs = this
    val errors: UploadPhotoServiceErrors = this

    private val compositeDisposable = CompositeDisposable()
    private val compositeJob = CompositeJob()
    private val MAX_ATTEMPTS = 3

    private val onPhotoUploadStatusOutput = PublishSubject.create<PhotoUploadingStatus>()

    private val badResponseError = PublishSubject.create<ServerErrorCode>()
    private val unknownErrorSubject = PublishSubject.create<Throwable>()

    override fun uploadPhotos() {
        compositeJob += async {
            try {
                val queuedUpPhotos = takenPhotosRepo.findAllQueuedUp().await()
                if (queuedUpPhotos.isEmpty()) {
                    onPhotoUploadStatusOutput.onNext(PhotoUploadingStatus.NoPhotoToUpload())
                    return@async
                }

                onPhotoUploadStatusOutput.onNext(PhotoUploadingStatus.StartPhotoUploading())

                for (photo in queuedUpPhotos) {
                    val photoName = uploadPhoto(photo)
                    if (photoName != null) {
                        photo.photoName = photoName
                        takenPhotosRepo.updateSetUploaded(photo.id, photoName)
                        onPhotoUploadStatusOutput.onNext(PhotoUploadingStatus.PhotoUploaded(photo))
                    } else {
                        takenPhotosRepo.updateSetFailedToUpload(photo.id).await()
                    }
                }

                //ensure that we receive AllPhotosUploaded event last
                delay(10, TimeUnit.MILLISECONDS)
                onPhotoUploadStatusOutput.onNext(PhotoUploadingStatus.AllPhotosUploaded())
            } catch (error: Throwable) {
                onPhotoUploadStatusOutput.onNext(PhotoUploadingStatus.UnknownErrorWhileUploading(error))
            }
        }
    }

    private suspend fun uploadPhoto(photo: TakenPhoto): String? {
        val response = RxUtils.repeatRequest(MAX_ATTEMPTS, PhotoToBeUploaded(photo.photoFilePath, photo.location, photo.userId)) { arg ->
            apiClient.uploadPhoto(arg).await()
        }

        if (response == null) {
            onPhotoUploadStatusOutput.onNext(PhotoUploadingStatus.FailedToUploadPhoto(photo))
            unknownErrorSubject.onNext(ApiException(ServerErrorCode.UNKNOWN_ERROR))
            return null
        }

        val errorCode = ServerErrorCode.from(response.serverErrorCode)
        if (errorCode != ServerErrorCode.OK) {
            onPhotoUploadStatusOutput.onNext(PhotoUploadingStatus.FailedToUploadPhoto(photo))
            badResponseError.onNext(errorCode)
            return null
        }

        FileUtils.deletePhotoFile(photo)
        takenPhotosRepo.updateSetUploaded(photo.id, response.photoName).await()

        return response.photoName
    }

    fun cleanUp() {
        compositeDisposable.clear()
        compositeJob.cancelAll()

        PhotoExchangeApplication.watch(this, this::class.simpleName)
        Timber.d("UploadPhotoServiceViewModel cleanUp")
    }

    override fun onPhotoUploadStatusObservable(): Observable<PhotoUploadingStatus> = onPhotoUploadStatusOutput

    override fun onBadResponseObservable(): Observable<ServerErrorCode> = badResponseError
    override fun onUnknownErrorObservable(): Observable<Throwable> = unknownErrorSubject
}



















