package com.kirakishou.photoexchange.mwvm.viewmodel

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mwvm.wires.errors.FindPhotoAnswerServiceErrors
import com.kirakishou.photoexchange.mwvm.wires.inputs.FindPhotoAnswerServiceInputs
import com.kirakishou.photoexchange.mwvm.wires.outputs.FindPhotoAnswerServiceOutputs
import com.kirakishou.photoexchange.mwvm.model.dto.PhotoAnswerReturnValue
import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer
import com.kirakishou.photoexchange.mwvm.model.other.ServerErrorCode
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.zipWith
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

/**
 * Created by kirakishou on 11/12/2017.
 */
class FindPhotoAnswerServiceViewModel(
        private val apiClient: ApiClient,
        private val schedulers: SchedulerProvider
) : FindPhotoAnswerServiceInputs,
        FindPhotoAnswerServiceOutputs,
        FindPhotoAnswerServiceErrors {

    val inputs: FindPhotoAnswerServiceInputs = this
    val outputs: FindPhotoAnswerServiceOutputs = this
    val errors: FindPhotoAnswerServiceErrors = this

    private val compositeDisposable = CompositeDisposable()

    private val noPhotosToSendBackSubject = PublishSubject.create<Unit>()
    private val userHasNoUploadedPhotosSubject = PublishSubject.create<Unit>()
    private val onPhotoAnswerFoundSubject = PublishSubject.create<PhotoAnswerReturnValue>()
    private val findPhotoAnswerSubject = PublishSubject.create<String>()
    private val badResponseSubject = PublishSubject.create<ServerErrorCode>()
    private val unknownErrorSubject = PublishSubject.create<Throwable>()

    init {
        fun setUpFindPhotoAnswer() {
            val responseObservable = findPhotoAnswerSubject
                    .subscribeOn(schedulers.provideIo())
                    .observeOn(schedulers.provideIo())
                    .flatMap { userId -> apiClient.findPhotoAnswer(userId).toObservable() }
                    .share()

            val responseErrorCode = responseObservable
                    .map { ServerErrorCode.from(it.serverErrorCode) }
                    .share()

            compositeDisposable += responseErrorCode
                    .filter { errorCode -> errorCode == ServerErrorCode.OK }
                    .zipWith(responseObservable)
                    .map { it.second }
                    .doOnNext {
                        //TODO: save in the DB
                    }
                    .map { response ->
                        val photoAnswerList = response.photoAnswerList.map { answer -> PhotoAnswer.fromPhotoAnswerJsonObject(answer) }
                        return@map PhotoAnswerReturnValue(photoAnswerList, response.allFound)
                    }
                    .subscribe(onPhotoAnswerFoundSubject::onNext, unknownErrorSubject::onNext)

            compositeDisposable += responseErrorCode
                    .filter { errorCode -> errorCode == ServerErrorCode.USER_HAS_NO_UPLOADED_PHOTOS }
                    .map { }
                    .subscribe(userHasNoUploadedPhotosSubject::onNext, unknownErrorSubject::onNext)

            compositeDisposable += responseErrorCode
                    .filter { errorCode -> errorCode == ServerErrorCode.NO_PHOTOS_TO_SEND_BACK }
                    .map { }
                    .subscribe(noPhotosToSendBackSubject::onNext, unknownErrorSubject::onNext)

            compositeDisposable += responseErrorCode
                    .filter { errorCode -> errorCode != ServerErrorCode.OK }
                    .filter { errorCode -> errorCode != ServerErrorCode.USER_HAS_NO_UPLOADED_PHOTOS }
                    .filter { errorCode -> errorCode != ServerErrorCode.NO_PHOTOS_TO_SEND_BACK }
                    .subscribe(badResponseSubject::onNext, unknownErrorSubject::onNext)
        }

        setUpFindPhotoAnswer()
    }

    override fun findPhotoAnswer(userId: String) {
        findPhotoAnswerSubject.onNext(userId)
    }

    fun cleanUp() {
        compositeDisposable.clear()

        Timber.d("FindPhotoAnswerServiceViewModel detached")
    }

    override fun userHasNoUploadedPhotosObservable(): Observable<Unit> = userHasNoUploadedPhotosSubject
    override fun noPhotosToSendBackObservable(): Observable<Unit> = noPhotosToSendBackSubject
    override fun onPhotoAnswerFoundObservable(): Observable<PhotoAnswerReturnValue> = onPhotoAnswerFoundSubject
    override fun onBadResponseObservable(): Observable<ServerErrorCode> = badResponseSubject
    override fun onUnknownErrorObservable(): Observable<Throwable> = unknownErrorSubject
}