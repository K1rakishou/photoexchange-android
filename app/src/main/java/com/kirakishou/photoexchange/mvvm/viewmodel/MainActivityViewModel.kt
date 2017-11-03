package com.kirakishou.photoexchange.mvvm.viewmodel

import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvvm.model.ErrorCode
import com.kirakishou.photoexchange.mvvm.model.LonLat
import com.kirakishou.photoexchange.mvvm.model.dto.PhotoWithLocation
import com.kirakishou.photoexchange.mvvm.model.net.response.SendPhotoResponse
import com.kirakishou.photoexchange.mvvm.model.net.response.StatusResponse
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.error.MainActivityViewModelErrors
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.input.MainActivityViewModelInputs
import com.kirakishou.photoexchange.mvvm.viewmodel.wires.output.MainActivityViewModelOutpus
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Created by kirakishou on 11/3/2017.
 */
class MainActivityViewModel
@Inject constructor(
        private val apiClient: ApiClient,
        private val schedulers: SchedulerProvider

) : BaseViewModel(),
        MainActivityViewModelInputs,
        MainActivityViewModelOutpus,
        MainActivityViewModelErrors {

    val inputs: MainActivityViewModelInputs = this
    val outputs: MainActivityViewModelOutpus = this
    val errors: MainActivityViewModelErrors = this

    private val sendPhotoSubject = PublishSubject.create<PhotoWithLocation>()

    init {
        compositeDisposable += sendPhotoSubject
                .subscribeOn(schedulers.provideIo())
                .flatMap { apiClient.sendPhoto<SendPhotoResponse>(it).toObservable() }
                .subscribe(this::handleResponse, this::handleError)
    }

    override fun onCleared() {
        Timber.e("ClientMainActivityViewModel.onCleared()")

        super.onCleared()
    }

    private fun handleResponse(response: StatusResponse) {
        val errorCode = response.errorCode

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
    }

    override fun sendPhoto(photoFile: File, location: LonLat) {
        sendPhotoSubject.onNext(PhotoWithLocation(photoFile, location))
    }
}