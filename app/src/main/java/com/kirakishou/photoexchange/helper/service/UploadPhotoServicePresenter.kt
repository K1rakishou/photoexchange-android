package com.kirakishou.photoexchange.helper.service

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.service.wires.errors.UploadPhotoServiceErrors
import com.kirakishou.photoexchange.helper.service.wires.inputs.UploadPhotoServiceInputs
import com.kirakishou.photoexchange.helper.service.wires.outputs.UploadPhotoServiceOutputs
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvvm.model.ServerErrorCode
import com.kirakishou.photoexchange.mvvm.model.LonLat
import com.kirakishou.photoexchange.mvvm.model.UploadedPhoto
import com.kirakishou.photoexchange.mvvm.model.dto.PhotoNameWithId
import com.kirakishou.photoexchange.mvvm.model.dto.PhotoToBeUploaded
import com.kirakishou.photoexchange.mvvm.model.exception.ApiException
import com.kirakishou.photoexchange.mvvm.model.exception.UnknownErrorCodeException
import com.kirakishou.photoexchange.mvvm.model.net.response.UploadPhotoResponse
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.zipWith
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

/**
 * Created by kirakishou on 11/4/2017.
 */


class UploadPhotoServicePresenter(
        private val apiClient: ApiClient,
        private val schedulers: SchedulerProvider,
        private val uploadedPhotosRepo: UploadedPhotosRepository
) : UploadPhotoServiceInputs, UploadPhotoServiceOutputs, UploadPhotoServiceErrors {

    private val compositeDisposable = CompositeDisposable()

    val inputs: UploadPhotoServiceInputs = this
    val outputs: UploadPhotoServiceOutputs = this
    val errors: UploadPhotoServiceErrors = this

    private val MAX_RETRY_TIMES = 3L

    private val sendPhotoResponseSubject = PublishSubject.create<UploadedPhoto>()
    private val sendPhotoRequestSubject = PublishSubject.create<PhotoToBeUploaded>()
    private val badResponseSubject = PublishSubject.create<ServerErrorCode>()
    private val unknownErrorSubject = PublishSubject.create<Throwable>()

    init {
        fun setUpSendPhotoRequestSubject(): Disposable {

            fun uploadPhoto(info: PhotoToBeUploaded): Observable<UploadPhotoResponse> {
                return apiClient.sendPhoto(info)
                        .doOnSuccess { Timber.d("Uploading packet with photo...") }
                        .retry(MAX_RETRY_TIMES)
                        .toObservable()
                        .doOnNext { response ->
                            val errorCode = ServerErrorCode.from(response.serverErrorCode)
                            if (errorCode != ServerErrorCode.OK) {
                                throw ApiException(errorCode)
                            }
                        }
            }

            fun cacheResponse(photoInfo: PhotoToBeUploaded): Observable<Long> {
                val location = photoInfo.location
                val photoFilePath = photoInfo.photoFilePath
                val userId = photoInfo.userId

                return uploadedPhotosRepo.saveOne(location.lon, location.lat, userId, photoFilePath).toObservable()
            }

            val photoInfoObservable = sendPhotoRequestSubject
                    .share()

            val responseObservable = photoInfoObservable
                    .subscribeOn(schedulers.provideIo())
                    .observeOn(schedulers.provideIo())
                    .doOnNext { AndroidUtils.throwIfOnMainThread() }
                    .flatMap { uploadPhoto(it) }
                    .share()

            val photoIdObservable = photoInfoObservable
                    .flatMap { cacheResponse(it) }

            val uploadedPhotoObservable = Observables.zip(photoInfoObservable, photoIdObservable, responseObservable)
                    .map {
                        val photoInfo = it.first
                        val photoId = it.second
                        val photoName = it.third.photoName

                        return@map UploadedPhoto(photoId, photoInfo.location.lon, photoInfo.location.lat,
                                photoInfo.userId, photoName, photoInfo.photoFilePath)
                    }

            return Observables.zip(responseObservable, uploadedPhotoObservable)
                    .onErrorReturn { error ->
                        if (error is ApiException) {
                            return@onErrorReturn Pair(UploadPhotoResponse.fail(error.serverErrorCode), UploadedPhoto.empty())
                        }

                        throw error
                    }
                    .subscribe({ handleSendPacketWithPhotoResponse(it.first, it.second) }, this::handleError)
        }

        compositeDisposable += setUpSendPhotoRequestSubject()
    }

    override fun uploadPhoto(photoFilePath: String, location: LonLat, userId: String) {
        sendPhotoRequestSubject.onNext(PhotoToBeUploaded(photoFilePath, location, userId))
    }

    private fun handleSendPacketWithPhotoResponse(response: UploadPhotoResponse, uploadedPhoto: UploadedPhoto) {
        val errorCode = ServerErrorCode.from(response.serverErrorCode)
        Timber.d("Received response, serverErrorCode: $errorCode")

        if (errorCode == ServerErrorCode.OK) {
            sendPhotoResponseSubject.onNext(uploadedPhoto)
        } else {
            when (errorCode) {
                ServerErrorCode.BAD_ERROR_CODE,
                ServerErrorCode.BAD_REQUEST,
                ServerErrorCode.DISK_ERROR,
                ServerErrorCode.REPOSITORY_ERROR,
                ServerErrorCode.UNKNOWN_ERROR -> {
                    badResponseSubject.onNext(errorCode)
                }

                else -> {
                    unknownErrorSubject.onNext(UnknownErrorCodeException(errorCode))
                }
            }
        }
    }

    private fun handleError(error: Throwable) {
        Timber.e(error)

        unknownErrorSubject.onNext(error)
    }

    fun detach() {
        compositeDisposable.clear()

        Timber.d("UploadPhotoServicePresenter detached")
    }

    override fun onSendPhotoResponseObservable(): Observable<UploadedPhoto> = sendPhotoResponseSubject
    override fun onBadResponseObservable(): Observable<ServerErrorCode> = badResponseSubject
    override fun onUnknownErrorObservable(): Observable<Throwable> = unknownErrorSubject
}



















