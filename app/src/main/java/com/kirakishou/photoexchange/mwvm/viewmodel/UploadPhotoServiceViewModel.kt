package com.kirakishou.photoexchange.mwvm.viewmodel

import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.helper.CompositeJob
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.util.FileUtils
import com.kirakishou.photoexchange.mwvm.model.dto.PhotoToBeUploaded
import com.kirakishou.photoexchange.mwvm.model.exception.ApiException
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

    private val onNoPhotosToUploadOutput = PublishSubject.create<Unit>()
    private val onAllPhotosUploadedOutput = PublishSubject.create<Unit>()
    private val onStartUploadQueuedUpPhotosOutput = PublishSubject.create<Unit>()
    private val sendPhotoResponseOutput = PublishSubject.create<TakenPhoto>()
    private val badResponseError = PublishSubject.create<ServerErrorCode>()
    private val failedToUploadPhotoOutput = PublishSubject.create<Long>()
    private val unknownErrorSubject = PublishSubject.create<Throwable>()

    override fun uploadPhotos() {
        compositeJob += async {
            try {
                val queuedUpPhotos = takenPhotosRepo.findAllQueuedUp().await()
                if (queuedUpPhotos.isEmpty()) {
                    onNoPhotosToUploadOutput.onNext(Unit)
                    return@async
                }

                onStartUploadQueuedUpPhotosOutput.onNext(Unit)

                for (queuedUpPhoto in queuedUpPhotos) {
                    val photoName = uploadPhoto(queuedUpPhoto)
                    if (photoName != null) {
                        queuedUpPhoto.photoName = photoName
                        sendPhotoResponseOutput.onNext(queuedUpPhoto)
                    }
                }

                //ensure that we receive AllPhotosUploaded event last
                delay(100, TimeUnit.MILLISECONDS)
                onAllPhotosUploadedOutput.onNext(Unit)
            } catch (error: Throwable) {
                sendPhotoResponseOutput.onError(error)
            }
        }
    }

    private suspend fun uploadPhoto(queuedUpPhoto: TakenPhoto): String? {
        val response = repeatRequest(MAX_ATTEMPTS, PhotoToBeUploaded(queuedUpPhoto.photoFilePath, queuedUpPhoto.location, queuedUpPhoto.userId)) { arg ->
            apiClient.uploadPhoto(arg).await()
        }

        if (response == null) {
            failedToUploadPhotoOutput.onNext(queuedUpPhoto.id)
            unknownErrorSubject.onNext(ApiException(ServerErrorCode.UNKNOWN_ERROR))
            return null
        }

        val errorCode = ServerErrorCode.from(response.serverErrorCode)
        if (errorCode != ServerErrorCode.OK) {
            failedToUploadPhotoOutput.onNext(queuedUpPhoto.id)
            badResponseError.onNext(errorCode)
            return null
        }

        FileUtils.deletePhotoFile(queuedUpPhoto)
        takenPhotosRepo.updateOneSetUploaded(queuedUpPhoto.id, response.photoName).await()

        return response.photoName
    }

    fun cleanUp() {
        compositeDisposable.clear()
        compositeJob.cancelAll()

        PhotoExchangeApplication.watch(this, this::class.simpleName)
        Timber.d("UploadPhotoServiceViewModel cleanUp")
    }

    private suspend fun <Argument, Result> repeatRequest(maxAttempts: Int, arg: Argument, block: suspend (arg: Argument) -> Result): Result? {
        var attempt = maxAttempts
        var response: Result? = null

        while (attempt-- > 0) {
            try {
                Timber.d("Trying to send request, attempt #${maxAttempts - attempt}")
                response = block(arg)
                return response!!
            } catch (error: Throwable) {
                Timber.e(error)
            }
        }

        return null
    }

    override fun onFailedToUploadPhotoObservable(): Observable<Long> = failedToUploadPhotoOutput
    override fun onNoPhotosToUploadObservable(): Observable<Unit> = onNoPhotosToUploadOutput
    override fun onStartUploadQueuedUpPhotosObservable(): Observable<Unit> = onStartUploadQueuedUpPhotosOutput
    override fun onAllPhotosUploadedObservable(): Observable<Unit> = onAllPhotosUploadedOutput
    override fun onUploadPhotoResponseObservable(): Observable<TakenPhoto> = sendPhotoResponseOutput
    override fun onBadResponseObservable(): Observable<ServerErrorCode> = badResponseError
    override fun onUnknownErrorObservable(): Observable<Throwable> = unknownErrorSubject
}



















