package com.kirakishou.photoexchange.mwvm.viewmodel

import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.database.repository.PhotoAnswerRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.util.FileUtils
import com.kirakishou.photoexchange.mwvm.model.other.Constants.ASYNC_DELAY
import com.kirakishou.photoexchange.mwvm.model.other.MulticastEvent
import com.kirakishou.photoexchange.mwvm.model.other.Pageable
import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer
import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto
import com.kirakishou.photoexchange.mwvm.model.state.LookingForPhotoState
import com.kirakishou.photoexchange.mwvm.model.state.PhotoUploadingState
import com.kirakishou.photoexchange.mwvm.wires.errors.AllPhotosViewActivityViewModelErrors
import com.kirakishou.photoexchange.mwvm.wires.inputs.AllPhotosViewActivityViewModelInputs
import com.kirakishou.photoexchange.mwvm.wires.outputs.AllPhotosViewActivityViewModelOutputs
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.rx2.asCompletable
import kotlinx.coroutines.experimental.rx2.await
import kotlinx.coroutines.experimental.rx2.awaitFirst
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Created by kirakishou on 11/7/2017.
 */
class AllPhotosViewActivityViewModel(
        private val photoAnswerRepository: PhotoAnswerRepository,
        private val takenPhotosRepository: TakenPhotosRepository,
        private val schedulers: SchedulerProvider
) : BaseViewModel(),
        AllPhotosViewActivityViewModelInputs,
        AllPhotosViewActivityViewModelOutputs,
        AllPhotosViewActivityViewModelErrors {

    private val tag = "[${this::class.java.simpleName}]: "

    val inputs: AllPhotosViewActivityViewModelInputs = this
    val outputs: AllPhotosViewActivityViewModelOutputs = this
    val errors: AllPhotosViewActivityViewModelErrors = this

    //inputs
    private val startLookingForPhotosInput = PublishSubject.create<Unit>()
    private val startPhotosUploadingInput = PublishSubject.create<Unit>()

    //outputs
    private val onUploadedPhotosPageReceivedOutput = PublishSubject.create<List<TakenPhoto>>()
    private val onReceivedPhotosPageReceivedOutput = PublishSubject.create<List<PhotoAnswer>>()
    private val scrollToTopOutput = PublishSubject.create<Unit>()
    private val showLookingForPhotoIndicatorOutput = PublishSubject.create<Unit>()
    private val startLookingForPhotosOutput = PublishSubject.create<Unit>()
    private val startPhotosUploadingOutput = PublishSubject.create<Unit>()
    private val onQueuedUpAndFailedToUploadLoadedOutput = PublishSubject.create<List<TakenPhoto>>()
    private val onTakenPhotoUploadingCanceledOutput = PublishSubject.create<Long>()
    private val beginReceivingEventsOutput = PublishSubject.create<Class<*>>()
    private val stopReceivingEventsOutput = PublishSubject.create<Class<*>>()
    private val showUploadMorePhotosMessageOutput = PublishSubject.create<Unit>()

    private val onPhotoUploadingStateOutput = PublishSubject.create<MulticastEvent<PhotoUploadingState>>()
    private val onLookingForPhotoStateOutput = PublishSubject.create<MulticastEvent<LookingForPhotoState>>()

    //errors
    private val unknownErrorSubject = PublishSubject.create<Throwable>()

    private val LOOK_FOR_PHOTOS_EVENT_TIMEOUT_SECONDS = 5L
    private val START_PHOTO_UPLOADING_EVENT_TIMEOUT_SECONDS = 5L

    init {
        compositeDisposable += startLookingForPhotosInput
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .delay(1, TimeUnit.SECONDS)
                .throttleFirst(LOOK_FOR_PHOTOS_EVENT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .flatMap { Singles.zip(takenPhotosRepository.countAll(), photoAnswerRepository.countAll()).toObservable() }
                .doOnNext { (takenPhotosCount, receivedPhotosCount) ->
                    val difference = takenPhotosCount - receivedPhotosCount

                    when {
                        difference > 0L -> {
                            Timber.tag(tag).d("startLookingForPhotosInput difference > 0L, start looking for photos")
                            startLookingForPhotosOutput.onNext(Unit)
                        }
                        difference == 0L -> {
                            Timber.tag(tag).d("startLookingForPhotosInput difference == 0L, do nothing")
                        }
                        difference < 0L -> {
                            Timber.tag(tag).d("startLookingForPhotosInput difference < 0L, do nothing")
                        }
                    }

                    if (receivedPhotosCount == 0L) {
                        Timber.tag(tag).d("receivedPhotosCount == 0L, show message that user needs to upload photos first")
                        showUploadMorePhotosMessageOutput.onNext(Unit)
                    }
                }
                .doOnError(startLookingForPhotosOutput::onError)
                .subscribe()

        compositeDisposable += startPhotosUploadingInput
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .delay(1, TimeUnit.SECONDS)
                .throttleFirst(START_PHOTO_UPLOADING_EVENT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .flatMap { takenPhotosRepository.countQueuedUp().toObservable() }
                .doOnNext { queuedUpCount ->
                    when {
                        queuedUpCount > 0L -> {
                            Timber.d("startPhotosUploadingInput queued up photos count > 0, start uploading")
                            startPhotosUploadingOutput.onNext(Unit)
                        }

                        else -> {
                            Timber.d("startPhotosUploadingInput queued up photos count <= 0, do nothing")
                        }
                    }
                }
                .doOnError(startPhotosUploadingOutput::onError)
                .subscribe()
    }

    override fun updatePhotoUploadingState(receiver: Class<*>, newState: PhotoUploadingState) {
        onPhotoUploadingStateOutput.onNext(MulticastEvent(receiver, newState))
    }

    override fun updateLookingForPhotoState(receiver: Class<*>, newState: LookingForPhotoState) {
        onLookingForPhotoStateOutput.onNext(MulticastEvent(receiver, newState))
    }

    override fun startPhotosUploading() {
        startPhotosUploadingInput.onNext(Unit)
    }

    fun markPhotoToBeUploadedAgain(photoId: Long) {
        compositeDisposable += async {
            try {
                takenPhotosRepository.updateSetQueuedUp(photoId).await()
                startPhotosUploadingInput.onNext(Unit)
            } catch (error: Throwable) {
                startPhotosUploadingInput.onError(error)
            }
        }.asCompletable(CommonPool).subscribe()
    }

    override fun beginReceivingEvents(clazz: Class<*>) {
        compositeDisposable += async {
            try {
                //FIXME: doesn't work without delay
                delay(ASYNC_DELAY, TimeUnit.MILLISECONDS)
                beginReceivingEventsOutput.onNext(clazz)
            } catch (error: Throwable) {
                beginReceivingEventsOutput.onError(error)
            }
        }.asCompletable(CommonPool).subscribe()
    }

    override fun stopReceivingEvents(clazz: Class<*>) {
        compositeDisposable += async {
            try {
                //FIXME: doesn't work without delay
                delay(ASYNC_DELAY, TimeUnit.MILLISECONDS)
                stopReceivingEventsOutput.onNext(clazz)
            } catch (error: Throwable) {
                stopReceivingEventsOutput.onError(error)
            }
        }.asCompletable(CommonPool).subscribe()
    }

    override fun fetchOnePageUploadedPhotos(page: Int, count: Int) {
        compositeDisposable += async {
            try {
                val uploadedPhotos = takenPhotosRepository.findOnePage(Pageable(page, count)).await()
                onUploadedPhotosPageReceivedOutput.onNext(uploadedPhotos)
            } catch (error: Throwable) {
                onUploadedPhotosPageReceivedOutput.onError(error)
            }
        }.asCompletable(CommonPool).subscribe()
    }

    override fun fetchOnePageReceivedPhotos(page: Int, count: Int) {
        compositeDisposable += async {
            try {
                val onePage = photoAnswerRepository.findOnePage(Pageable(page, count)).awaitFirst()
                onReceivedPhotosPageReceivedOutput.onNext(onePage)
            } catch (error: Throwable) {
                onReceivedPhotosPageReceivedOutput.onError(error)
            }
        }.asCompletable(CommonPool).subscribe()
    }

    override fun scrollToTop() {
        compositeDisposable += async {
            try {
                //FIXME: doesn't work without delay
                delay(ASYNC_DELAY, TimeUnit.MILLISECONDS)
                scrollToTopOutput.onNext(Unit)
            } catch (error: Throwable) {
                scrollToTopOutput.onError(error)
            }
        }.asCompletable(CommonPool).subscribe()
    }

    override fun showLookingForPhotoIndicator() {
        compositeDisposable += async {
            showLookingForPhotoIndicatorOutput.onNext(Unit)
        }.asCompletable(CommonPool).subscribe()
    }

    override fun startLookingForPhotos() {
        startLookingForPhotosInput.onNext(Unit)
    }

    override fun getQueuedUpAndFailedToUploadPhotos() {
        compositeDisposable += async {
            //FIXME: doesn't work without delay
            delay(ASYNC_DELAY, TimeUnit.MILLISECONDS)

            try {
                val zippedPhotos = Singles.zip(
                        takenPhotosRepository.findAllQueuedUp(),
                        takenPhotosRepository.findAllFailedToUpload()) { queuedUpPhotos, failedToUploadPhotos ->

                    val resultList = mutableListOf<TakenPhoto>()
                    resultList.addAll(failedToUploadPhotos)
                    resultList.addAll(queuedUpPhotos)

                    return@zip resultList
                }.subscribeOn(schedulers.provideIo()).await()

                onQueuedUpAndFailedToUploadLoadedOutput.onNext(zippedPhotos)
            } catch (error: Throwable) {
                onQueuedUpAndFailedToUploadLoadedOutput.onError(error)
            }
        }.asCompletable(CommonPool).subscribe()
    }

    override fun cancelTakenPhotoUploading(id: Long) {
        compositeDisposable += async {
            try {
                val takenPhoto = takenPhotosRepository.findOne(id).await()
                FileUtils.deletePhotoFile(takenPhoto)
                takenPhotosRepository.deleteOne(id).await()

                onTakenPhotoUploadingCanceledOutput.onNext(id)
            } catch (error: Throwable) {
                onTakenPhotoUploadingCanceledOutput.onError(error)
            }
        }.asCompletable(CommonPool).subscribe()
    }

    private fun handleErrors(error: Throwable) {
        Timber.e(error)
        unknownErrorSubject.onNext(error)
    }

    override fun onCleared() {
        Timber.tag(tag).d("onCleared()")

        super.onCleared()
    }

    override fun onTakenPhotoUploadingCanceledObservable(): Observable<Long> = onTakenPhotoUploadingCanceledOutput
    override fun onUploadedPhotosPageReceivedObservable(): Observable<List<TakenPhoto>> = onUploadedPhotosPageReceivedOutput
    override fun onReceivedPhotosPageReceivedObservable(): Observable<List<PhotoAnswer>> = onReceivedPhotosPageReceivedOutput
    override fun onScrollToTopObservable(): Observable<Unit> = scrollToTopOutput
    override fun onShowLookingForPhotoIndicatorObservable(): Observable<Unit> = showLookingForPhotoIndicatorOutput
    override fun onStartLookingForPhotosObservable(): Observable<Unit> = startLookingForPhotosOutput
    override fun onStartPhotosUploadingObservable(): Observable<Unit> = startPhotosUploadingOutput
    override fun onQueuedUpAndFailedToUploadLoadedObservable(): Observable<List<TakenPhoto>> = onQueuedUpAndFailedToUploadLoadedOutput
    override fun onBeginReceivingEventsObservable(): Observable<Class<*>> = beginReceivingEventsOutput
    override fun onStopReceivingEventsObservable(): Observable<Class<*>> = stopReceivingEventsOutput
    override fun onShowUploadMorePhotosMessageObservable(): Observable<Unit> = showUploadMorePhotosMessageOutput

    override fun onPhotoUploadingStateObservable(): Observable<MulticastEvent<PhotoUploadingState>> = onPhotoUploadingStateOutput
    override fun onLookingForPhotoStateObservable(): Observable<MulticastEvent<LookingForPhotoState>> = onLookingForPhotoStateOutput

    override fun onUnknownErrorObservable(): Observable<Throwable> = unknownErrorSubject
}
















