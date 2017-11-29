package com.kirakishou.photoexchange.mwvm.viewmodel

import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.helper.CompositeJob
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.repository.PhotoAnswerRepository
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mwvm.wires.errors.FindPhotoAnswerServiceErrors
import com.kirakishou.photoexchange.mwvm.wires.inputs.FindPhotoAnswerServiceInputs
import com.kirakishou.photoexchange.mwvm.wires.outputs.FindPhotoAnswerServiceOutputs
import com.kirakishou.photoexchange.mwvm.model.dto.PhotoAnswerReturnValue
import com.kirakishou.photoexchange.mwvm.model.other.Constants
import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer
import com.kirakishou.photoexchange.mwvm.model.other.ServerErrorCode
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.rx2.await
import timber.log.Timber

/**
 * Created by kirakishou on 11/12/2017.
 */
class FindPhotoAnswerServiceViewModel(
        private val photoAnswerRepo: PhotoAnswerRepository,
        private val apiClient: ApiClient,
        private val schedulers: SchedulerProvider
) : FindPhotoAnswerServiceInputs,
    FindPhotoAnswerServiceOutputs,
    FindPhotoAnswerServiceErrors {

    val inputs: FindPhotoAnswerServiceInputs = this
    val outputs: FindPhotoAnswerServiceOutputs = this
    val errors: FindPhotoAnswerServiceErrors = this

    private val compositeDisposable = CompositeDisposable()
    private val compositeJob = CompositeJob()

    //inputs

    //outputs
    private val uploadMorePhotosSubject = PublishSubject.create<Unit>()
    private val couldNotMarkPhotoAsReceivedSubject = PublishSubject.create<Unit>()
    private val noPhotosToSendBackSubject = PublishSubject.create<Unit>()
    private val onPhotoAnswerFoundSubject = PublishSubject.create<PhotoAnswerReturnValue>()

    //errors
    private val badResponseSubject = PublishSubject.create<ServerErrorCode>()
    private val unknownErrorSubject = PublishSubject.create<Throwable>()

    override fun findPhotoAnswer(userId: String) {
        compositeJob += async {
            try {
                val findPhotoResponse = apiClient.findPhotoAnswer(userId).await()
                val findPhotoErrorCode = ServerErrorCode.from(findPhotoResponse.serverErrorCode)

                when (findPhotoErrorCode) {
                    ServerErrorCode.NO_PHOTOS_TO_SEND_BACK -> noPhotosToSendBackSubject.onNext(Unit)
                    ServerErrorCode.UPLOAD_MORE_PHOTOS -> uploadMorePhotosSubject.onNext(Unit)
                    ServerErrorCode.OK -> {
                        val photoAnswer = PhotoAnswer.fromPhotoAnswerJsonObject(findPhotoResponse.photoAnswer!!)
                        val markPhotoResponse = apiClient.markPhotoAsReceived(photoAnswer.photoRemoteId, userId).await()

                        val markPhotoErrorCode = ServerErrorCode.from(markPhotoResponse.serverErrorCode)
                        if (markPhotoErrorCode != ServerErrorCode.OK) {
                            couldNotMarkPhotoAsReceivedSubject.onNext(Unit)
                        } else {
                            photoAnswerRepo.saveOne(photoAnswer).await()

                            if (Constants.isDebugBuild) {
                                val allPhotoAnswers = photoAnswerRepo.findAll().await()
                                allPhotoAnswers.forEach { Timber.d(it.toString()) }
                            }

                            onPhotoAnswerFoundSubject.onNext(PhotoAnswerReturnValue(photoAnswer, findPhotoResponse.allFound))
                        }
                    }

                    else -> badResponseSubject.onNext(findPhotoErrorCode)
                }
            } catch (error: Throwable) {
                onPhotoAnswerFoundSubject.onError(error)
            }
        }
    }

    private fun handleErrors(error: Throwable) {
        Timber.e(error)
        unknownErrorSubject.onNext(error)
    }

    fun cleanUp() {
        compositeDisposable.clear()
        compositeJob.cancelAll()

        PhotoExchangeApplication.watch(this, this::class.simpleName)
        Timber.d("FindPhotoAnswerServiceViewModel cleanUp")
    }

    //outputs
    override fun uploadMorePhotosObservable(): Observable<Unit> = uploadMorePhotosSubject
    override fun couldNotMarkPhotoAsReceivedObservable(): Observable<Unit> = couldNotMarkPhotoAsReceivedSubject
    override fun noPhotosToSendBackObservable(): Observable<Unit> = noPhotosToSendBackSubject
    override fun onPhotoAnswerFoundObservable(): Observable<PhotoAnswerReturnValue> = onPhotoAnswerFoundSubject

    //errors
    override fun onBadResponseObservable(): Observable<ServerErrorCode> = badResponseSubject
    override fun onUnknownErrorObservable(): Observable<Throwable> = unknownErrorSubject
}