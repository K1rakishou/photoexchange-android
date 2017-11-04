package com.kirakishou.photoexchange.helper.service

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.service.wires.errors.SendPhotoServiceErrors
import com.kirakishou.photoexchange.helper.service.wires.inputs.SendPhotoServiceInputs
import com.kirakishou.photoexchange.helper.service.wires.outputs.SendPhotoServiceOutpus
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvvm.model.ErrorCode
import com.kirakishou.photoexchange.mvvm.model.LonLat
import com.kirakishou.photoexchange.mvvm.model.dto.PhotoWithInfo
import com.kirakishou.photoexchange.mvvm.model.net.response.SendPhotoResponse
import com.kirakishou.photoexchange.mvvm.model.net.response.StatusResponse
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.io.File

/**
 * Created by kirakishou on 11/4/2017.
 */


class SendPhotoServicePresenter(
        private val apiClient: ApiClient,
        private val schedulers: SchedulerProvider
) : SendPhotoServiceInputs, SendPhotoServiceOutpus, SendPhotoServiceErrors {

    private val compositeDisposable = CompositeDisposable()

    val inputs: SendPhotoServiceInputs = this
    val outputs: SendPhotoServiceOutpus = this
    val errors: SendPhotoServiceErrors = this

    private val sendPhotoSubject = PublishSubject.create<PhotoWithInfo>()
    private val onBadResponse = PublishSubject.create<ErrorCode>()
    private val unknownErrorSubject = PublishSubject.create<Throwable>()

    init {
        compositeDisposable += sendPhotoSubject
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

    override fun uploadPhoto(photoFile: File, location: LonLat, userId: String) {
        sendPhotoSubject.onNext(PhotoWithInfo(photoFile, location, userId))
    }

    private fun handleResponse(response: StatusResponse) {
        val errorCode = response.errorCode
        Timber.d("Received response, errorCode: $errorCode")

        when (response) {
            is SendPhotoResponse -> {
                if (errorCode == ErrorCode.REC_OK) {

                } else {

                }
            }

            else -> RuntimeException("Unknown response")
        }
    }

    private fun handleError(error: Throwable) {
        Timber.e(error)

        unknownErrorSubject.onNext(error)
    }
}