package com.kirakishou.photoexchange.helper.service

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.service.wires.errors.FindPhotoAnswerServiceErrors
import com.kirakishou.photoexchange.helper.service.wires.inputs.FindPhotoAnswerServiceInputs
import com.kirakishou.photoexchange.helper.service.wires.outputs.FindPhotoAnswerServiceOutputs
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

    private val onPhotoAnswerFoundSubject = PublishSubject.create<Unit>()
    private val findPhotoAnswerSubject = PublishSubject.create<String>()
    private val unknownErrorSubject = PublishSubject.create<Throwable>()

    init {
        compositeDisposable += findPhotoAnswerSubject
                .subscribeOn(schedulers.provideIo())
                .observeOn(schedulers.provideIo())
                .flatMap { userId -> apiClient.findPhotoAnswer(userId).toObservable() }
                .subscribe()
    }

    override fun findPhotoAnswer(userId: String) {
        findPhotoAnswerSubject.onNext(userId)
    }

    private fun handleError(error: Throwable) {
        Timber.e(error)

        unknownErrorSubject.onNext(error)
    }

    fun detach() {
        compositeDisposable.clear()

        Timber.d("FindPhotoAnswerServicePresenter detached")
    }

    override fun onUnknownErrorObservable(): Observable<Throwable> = unknownErrorSubject
}