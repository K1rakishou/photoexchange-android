package com.kirakishou.photoexchange.mwvm.viewmodel

import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.helper.CompositeJob
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.repository.PhotoAnswerRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mwvm.wires.errors.FindPhotoAnswerServiceErrors
import com.kirakishou.photoexchange.mwvm.wires.inputs.FindPhotoAnswerServiceInputs
import com.kirakishou.photoexchange.mwvm.wires.outputs.FindPhotoAnswerServiceOutputs
import com.kirakishou.photoexchange.mwvm.model.dto.PhotoAnswerReturnValue
import com.kirakishou.photoexchange.mwvm.model.exception.ApiException
import com.kirakishou.photoexchange.mwvm.model.other.Constants
import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer
import com.kirakishou.photoexchange.mwvm.model.other.ServerErrorCode
import com.kirakishou.photoexchange.mwvm.model.state.FindPhotoState
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
        private val photoAnswerRepository: PhotoAnswerRepository,
        private val takenPhotosRepository: TakenPhotosRepository,
        private val apiClient: ApiClient,
        private val schedulers: SchedulerProvider
) : FindPhotoAnswerServiceInputs,
    FindPhotoAnswerServiceOutputs,
    FindPhotoAnswerServiceErrors {

    private val tag = "[${this::class.java.simpleName}]: "

    val inputs: FindPhotoAnswerServiceInputs = this
    val outputs: FindPhotoAnswerServiceOutputs = this
    val errors: FindPhotoAnswerServiceErrors = this

    private val compositeDisposable = CompositeDisposable()
    private val compositeJob = CompositeJob()

    //outputs
    private val findPhotoStateOutput = PublishSubject.create<FindPhotoState>()

    //errors
    private val badResponseSubject = PublishSubject.create<ServerErrorCode>()
    private val unknownErrorSubject = PublishSubject.create<Throwable>()

    override fun findPhotoAnswer(userId: String) {
        compositeJob += async {
            try {
                val receivedCount = photoAnswerRepository.countAll().await()
                val uploadedCount = takenPhotosRepository.countAll().await()

                if (uploadedCount <= receivedCount) {
                    findPhotoStateOutput.onNext(FindPhotoState.UploadMorePhotos())
                    return@async
                }

                val findPhotoResponse = apiClient.findPhotoAnswer(userId).await()
                val findPhotoErrorCode = ServerErrorCode.from(findPhotoResponse.serverErrorCode)

                when (findPhotoErrorCode) {
                    ServerErrorCode.NO_PHOTOS_TO_SEND_BACK -> findPhotoStateOutput.onNext(FindPhotoState.ServerHasNoPhotos())
                    ServerErrorCode.UPLOAD_MORE_PHOTOS -> findPhotoStateOutput.onNext(FindPhotoState.UploadMorePhotos())
                    ServerErrorCode.OK -> {
                        val photoAnswer = PhotoAnswer.fromPhotoAnswerJsonObject(findPhotoResponse.photoAnswer!!)
                        val markPhotoResponse = apiClient.markPhotoAsReceived(photoAnswer.photoRemoteId, userId).await()
                        val markPhotoErrorCode = ServerErrorCode.from(markPhotoResponse.serverErrorCode)

                        if (markPhotoErrorCode != ServerErrorCode.OK) {
                            findPhotoStateOutput.onNext(FindPhotoState.LocalRepositoryError())
                        } else {
                            photoAnswerRepository.saveOne(photoAnswer).await()

                            if (Constants.isDebugBuild) {
                                val allPhotoAnswers = photoAnswerRepository.findAll().await()
                                allPhotoAnswers.forEach { Timber.tag(tag).d(it.toString()) }
                            }

                            findPhotoStateOutput.onNext(FindPhotoState.PhotoFound(photoAnswer, findPhotoResponse.allFound))
                        }
                    }

                    else -> findPhotoStateOutput.onNext(FindPhotoState.UnknownError(ApiException(findPhotoErrorCode)))
                }
            } catch (error: Throwable) {
                findPhotoStateOutput.onNext(FindPhotoState.UnknownError(error))
            }
        }
    }

    fun cleanUp() {
        compositeDisposable.clear()
        compositeJob.cancelAll()

        //PhotoExchangeApplication.refWatcher!!.watch(this, this::class.java.simpleName)
        Timber.tag(tag).d("cleanUp()")
    }

    //outputs
    override fun findPhotoStateObservable(): Observable<FindPhotoState> = findPhotoStateOutput

    //errors
    override fun onBadResponseObservable(): Observable<ServerErrorCode> = badResponseSubject
    override fun onUnknownErrorObservable(): Observable<Throwable> = unknownErrorSubject
}