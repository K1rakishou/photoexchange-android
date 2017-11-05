package com.kirakishou.photoexchange.helper.service

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.service.wires.errors.SendPhotoServiceErrors
import com.kirakishou.photoexchange.helper.service.wires.inputs.SendPhotoServiceInputs
import com.kirakishou.photoexchange.helper.service.wires.outputs.SendPhotoServiceOutputs
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvvm.model.ServerErrorCode
import com.kirakishou.photoexchange.mvvm.model.LonLat
import com.kirakishou.photoexchange.mvvm.model.dto.PhotoWithInfo
import com.kirakishou.photoexchange.mvvm.model.net.response.SendPhotoResponse
import com.kirakishou.photoexchange.mvvm.model.net.response.StatusResponse
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

/**
 * Created by kirakishou on 11/4/2017.
 */


class SendPhotoServicePresenter(
        private val apiClient: ApiClient,
        private val schedulers: SchedulerProvider
) : SendPhotoServiceInputs, SendPhotoServiceOutputs, SendPhotoServiceErrors {

    private val compositeDisposable = CompositeDisposable()

    val inputs: SendPhotoServiceInputs = this
    val outputs: SendPhotoServiceOutputs = this
    val errors: SendPhotoServiceErrors = this

    private val sendPhotoResponseSubject = PublishSubject.create<ServerErrorCode>()
    private val sendPhotoRequestSubject = PublishSubject.create<PhotoWithInfo>()
    private val badResponseSubject = PublishSubject.create<ServerErrorCode>()
    private val unknownErrorSubject = PublishSubject.create<Throwable>()

    init {
        compositeDisposable += sendPhotoRequestSubject
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .doOnNext { AndroidUtils.throwIfOnMainThread() }
                .flatMap { info -> apiClient.sendPhoto(info).toObservable() }
                .subscribe(this::handleResponse, this::handleError)
    }

    fun detach() {
        compositeDisposable.clear()

        Timber.d("SendPhotoServicePresenter detached")
    }

    override fun uploadPhoto(photoFilePath: String, location: LonLat, userId: String) {
        sendPhotoRequestSubject.onNext(PhotoWithInfo(photoFilePath, location, userId))
    }

    private fun handleResponse(response: StatusResponse) {
        val errorCode = ServerErrorCode.from(response.serverErrorCode)
        Timber.d("Received response, serverErrorCode: $errorCode")

        when (response) {
            is SendPhotoResponse -> {
                if (errorCode == ServerErrorCode.REC_OK) {
                    sendPhotoResponseSubject.onNext(errorCode)
                } else {
                    badResponseSubject.onNext(errorCode)
                }
            }

            else -> RuntimeException("Unknown response")
        }
    }

    private fun handleError(error: Throwable) {
        Timber.e(error)

        unknownErrorSubject.onNext(error)
    }

    override fun onSendPhotoResponseObservable(): Observable<ServerErrorCode> = sendPhotoResponseSubject
    override fun onBadResponseObservable(): Observable<ServerErrorCode> = badResponseSubject
    override fun onUnknownErrorObservable(): Observable<Throwable> = unknownErrorSubject
}



















