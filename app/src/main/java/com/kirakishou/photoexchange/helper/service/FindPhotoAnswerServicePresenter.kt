package com.kirakishou.photoexchange.helper.service

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.service.wires.errors.FindPhotoAnswerServiceErrors
import com.kirakishou.photoexchange.helper.service.wires.inputs.FindPhotoAnswerServiceInputs
import com.kirakishou.photoexchange.helper.service.wires.outputs.FindPhotoAnswerServiceOutputs
import com.kirakishou.photoexchange.mvvm.model.other.PhotoAnswer
import com.kirakishou.photoexchange.mvvm.model.other.ServerErrorCode
import com.kirakishou.photoexchange.mvvm.model.dto.PhotoAnswerReturnValue
import com.kirakishou.photoexchange.mvvm.model.exception.UnknownErrorCodeException
import com.kirakishou.photoexchange.mvvm.model.net.response.PhotoAnswerResponse
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

/**
 * Created by kirakishou on 11/12/2017.
 */
class FindPhotoAnswerServicePresenter(
    private val apiClient: ApiClient,
    private val schedulers: SchedulerProvider
) : FindPhotoAnswerServiceInputs,
        FindPhotoAnswerServiceOutputs,
        FindPhotoAnswerServiceErrors {

    val inputs: FindPhotoAnswerServiceInputs = this
    val outputs: FindPhotoAnswerServiceOutputs = this
    val errors: FindPhotoAnswerServiceErrors = this

    private val compositeDisposable = CompositeDisposable()

    private val onPhotoAnswerFoundSubject = PublishSubject.create<PhotoAnswerReturnValue>()
    private val findPhotoAnswerSubject = PublishSubject.create<String>()
    private val badResponseSubject = PublishSubject.create<ServerErrorCode>()
    private val unknownErrorSubject = PublishSubject.create<Throwable>()

    init {
        compositeDisposable += findPhotoAnswerSubject
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .flatMap { userId -> apiClient.findPhotoAnswer(userId).toObservable() }
                .doOnNext {
                    //TODO: save PhotoAnswerJsonObject in the DB
                }
                .subscribe(this::handleFindPhotoAnswerResponse, this::handleError)
    }

    override fun findPhotoAnswer(userId: String) {
        findPhotoAnswerSubject.onNext(userId)
    }

    private fun handleFindPhotoAnswerResponse(response: PhotoAnswerResponse) {
        val errorCode = ServerErrorCode.from(response.serverErrorCode)
        Timber.d("Received response, serverErrorCode: $errorCode")

        if (errorCode == ServerErrorCode.OK) {
            //TODO: use mapper instead

            val photoAnswerList = response.photoAnswerList.map { PhotoAnswer(it.userId, it.photoName, it.lon, it.lat) }
            val returnValue = PhotoAnswerReturnValue(photoAnswerList, response.allFound)

            onPhotoAnswerFoundSubject.onNext(returnValue)
        } else {
            when (errorCode) {
                ServerErrorCode.BAD_ERROR_CODE,
                ServerErrorCode.BAD_REQUEST,
                ServerErrorCode.DISK_ERROR,
                ServerErrorCode.REPOSITORY_ERROR,
                ServerErrorCode.NOTHING_FOUND,
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

        Timber.d("FindPhotoAnswerServicePresenter detached")
    }

    override fun onPhotoAnswerFoundObservable(): Observable<PhotoAnswerReturnValue> = onPhotoAnswerFoundSubject
    override fun onBadResponseObservable(): Observable<ServerErrorCode> = badResponseSubject
    override fun onUnknownErrorObservable(): Observable<Throwable> = unknownErrorSubject
}