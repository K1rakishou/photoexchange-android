package com.kirakishou.photoexchange.mwvm.viewmodel

import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.database.repository.PhotoAnswerRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.util.FileUtils
import com.kirakishou.photoexchange.mwvm.model.dto.PhotoAnswerAllFound
import com.kirakishou.photoexchange.mwvm.model.other.MulticastEvent
import com.kirakishou.photoexchange.mwvm.model.other.Pageable
import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer
import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto
import com.kirakishou.photoexchange.mwvm.wires.errors.AllPhotosViewActivityViewModelErrors
import com.kirakishou.photoexchange.mwvm.wires.inputs.AllPhotosViewActivityViewModelInputs
import com.kirakishou.photoexchange.mwvm.wires.outputs.AllPhotosViewActivityViewModelOutputs
import io.reactivex.Observable
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
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

    //outputs
    private val startUploadingPhotosOutput = PublishSubject.create<MulticastEvent<Unit>>()
    private val onUploadedPhotosPageReceivedOutput = PublishSubject.create<List<TakenPhoto>>()
    private val onReceivedPhotosPageReceivedOutput = PublishSubject.create<List<PhotoAnswer>>()
    private val scrollToTopOutput = PublishSubject.create<Unit>()
    private val showLookingForPhotoIndicatorOutput = PublishSubject.create<Unit>()
    private val showPhotoUploadedOutput = PublishSubject.create<MulticastEvent<TakenPhoto>>()
    private val showFailedToUploadPhotoOutput = PublishSubject.create<MulticastEvent<TakenPhoto>>()
    private val showPhotoReceivedOutput = PublishSubject.create<PhotoAnswerAllFound>()
    private val showErrorWhileTryingToLookForPhotoOutput = PublishSubject.create<Unit>()
    private val showNoPhotoOnServerOutput = PublishSubject.create<Unit>()
    private val showUserNeedsToUploadMorePhotosOutput = PublishSubject.create<Unit>()
    private val startLookingForPhotosOutput = PublishSubject.create<Unit>()
    private val onQueuedUpAndFailedToUploadLoadedOutput = PublishSubject.create<List<TakenPhoto>>()
    private val allPhotosUploadedOutput = PublishSubject.create<MulticastEvent<Unit>>()
    private val showNoUploadedPhotosOutput = PublishSubject.create<Unit>()
    private val onTakenPhotoUploadingCanceledOutput = PublishSubject.create<Long>()
    private val beginReceivingEventsOutput = PublishSubject.create<Class<*>>()
    private val stopReceivingEventsOutput = PublishSubject.create<Class<*>>()
    private val onPhotoMarkedToBeUploadedOutput = PublishSubject.create<Unit>()

    //errors
    private val unknownErrorSubject = PublishSubject.create<Throwable>()

    fun markPhotoToBeUploadedAgain(photoId: Long) {
        compositeJob += async {
            try {
                takenPhotosRepository.updateSetQueuedUp(photoId).await()

                onPhotoMarkedToBeUploadedOutput.onNext(Unit)
            } catch (error: Throwable) {
                onPhotoMarkedToBeUploadedOutput.onError(error)
            }
        }
    }

    override fun beginReceivingEvents(clazz: Class<*>) {
        compositeJob += async {
            try {
                delay(300, TimeUnit.MILLISECONDS)
                beginReceivingEventsOutput.onNext(clazz)
            } catch (error: Throwable) {
                beginReceivingEventsOutput.onError(error)
            }
        }
    }

    override fun stopReceivingEvents(clazz: Class<*>) {
        compositeJob += async {
            try {
                delay(300, TimeUnit.MILLISECONDS)
                stopReceivingEventsOutput.onNext(clazz)
            } catch (error: Throwable) {
                stopReceivingEventsOutput.onError(error)
            }
        }
    }

    override fun fetchOnePageUploadedPhotos(page: Int, count: Int) {
        compositeJob += async {
            try {
                val uploadedPhotos = takenPhotosRepository.findOnePage(Pageable(page, count)).await()
                onUploadedPhotosPageReceivedOutput.onNext(uploadedPhotos)
            } catch (error: Throwable) {
                onUploadedPhotosPageReceivedOutput.onError(error)
            }
        }
    }

    override fun fetchOnePageReceivedPhotos(page: Int, count: Int) {
        compositeJob += async {
            try {
                val onePage = photoAnswerRepository.findOnePage(Pageable(page, count)).awaitFirst()
                onReceivedPhotosPageReceivedOutput.onNext(onePage)
            } catch (error: Throwable) {
                onReceivedPhotosPageReceivedOutput.onError(error)
            }
        }
    }

    override fun scrollToTop() {
        compositeJob += async {
            try {
                delay(500, TimeUnit.MILLISECONDS)
                scrollToTopOutput.onNext(Unit)
            } catch (error: Throwable) {
                scrollToTopOutput.onError(error)
            }
        }
    }

    override fun showLookingForPhotoIndicator() {
        compositeJob += async {
            showLookingForPhotoIndicatorOutput.onNext(Unit)
        }
    }

    override fun startUploadingPhotos(receiver: Class<*>) {
        compositeJob += async {
            startUploadingPhotosOutput.onNext(MulticastEvent(receiver, Unit))
        }
    }

    override fun photoUploaded(receiver: Class<*>, photo: TakenPhoto) {
        compositeJob += async {
            showPhotoUploadedOutput.onNext(MulticastEvent(receiver, photo))
        }
    }

    override fun showFailedToUploadPhoto(receiver: Class<*>, photo: TakenPhoto) {
        compositeJob += async {
            showFailedToUploadPhotoOutput.onNext(MulticastEvent(receiver, photo))
        }
    }

    override fun showPhotoReceived(photo: PhotoAnswer, allFound: Boolean) {
        compositeJob += async {
            showPhotoReceivedOutput.onNext(PhotoAnswerAllFound(photo, allFound))
        }
    }

    override fun showErrorWhileTryingToLookForPhoto() {
        compositeJob += async {
            showErrorWhileTryingToLookForPhotoOutput.onNext(Unit)
        }
    }

    override fun showNoPhotoOnServer() {
        compositeJob += async {
            showNoPhotoOnServerOutput.onNext(Unit)
        }
    }

    override fun showUserNeedsToUploadMorePhotos() {
        compositeJob += async {
            showUserNeedsToUploadMorePhotosOutput.onNext(Unit)
        }
    }

    override fun shouldStartLookingForPhotos() {
        compositeJob += async {
            try {
                //FIXME: rx chain doesn't work without delay
                delay(500, TimeUnit.MILLISECONDS)

                val receivedCount = photoAnswerRepository.countAll().await()
                val uploadedCount = takenPhotosRepository.countAll().await()

                when {
                    uploadedCount == 0L -> {
                        Timber.tag(tag).d("shouldStartLookingForPhotos() No uploaded photos, show a message")
                        showNoUploadedPhotosOutput.onNext(Unit)
                    }
                    uploadedCount > receivedCount -> {
                        Timber.tag(tag).d("shouldStartLookingForPhotos() uploadedCount GREATER THAN receivedCount")
                        startLookingForPhotosOutput.onNext(Unit)
                    }
                    else -> Timber.tag(tag).d("shouldStartLookingForPhotos() uploadedCount LESS OR EQUALS THAN receivedCount")
                }
            } catch (error: Throwable) {
                startLookingForPhotosOutput.onError(error)
            }
        }
    }

    override fun getQueuedUpAndFailedToUploadPhotos() {
        compositeJob += async {
            //FIXME: rx chain doesn't work without delay
            delay(500, TimeUnit.MILLISECONDS)

            try {
                //rxjava is still a more convenient way to start concurrent requests
                val zippedPhotos = Singles.zip(
                        takenPhotosRepository.findAllQueuedUp(),
                        takenPhotosRepository.findAllFailedToUpload()) { queuedUpPhotos, failedToUploadPhotos ->

                    val resultList = mutableListOf<TakenPhoto>()
                    resultList.addAll(failedToUploadPhotos)
                    resultList.addAll(queuedUpPhotos)

                    return@zip resultList
                }.await()

                onQueuedUpAndFailedToUploadLoadedOutput.onNext(zippedPhotos)

            } catch (error: Throwable) {
                onQueuedUpAndFailedToUploadLoadedOutput.onError(error)
            }
        }
    }

    override fun allPhotosUploaded(receiver: Class<*>) {
        compositeJob += async {
            allPhotosUploadedOutput.onNext(MulticastEvent(receiver, Unit))
        }
    }

    override fun cancelTakenPhotoUploading(id: Long) {
        compositeJob += async {
            try {
                val takenPhoto = takenPhotosRepository.findOne(id).await()
                FileUtils.deletePhotoFile(takenPhoto)
                takenPhotosRepository.deleteOne(id).await()

                onTakenPhotoUploadingCanceledOutput.onNext(id)
            } catch (error: Throwable) {
                onTakenPhotoUploadingCanceledOutput.onError(error)
            }
        }
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
    override fun onShowPhotoReceivedObservable(): Observable<PhotoAnswerAllFound> = showPhotoReceivedOutput
    override fun onShowErrorWhileTryingToLookForPhotoObservable(): Observable<Unit> = showErrorWhileTryingToLookForPhotoOutput
    override fun onShowNoPhotoOnServerObservable(): Observable<Unit> = showNoPhotoOnServerOutput
    override fun onShowUserNeedsToUploadMorePhotosObservable(): Observable<Unit> = showUserNeedsToUploadMorePhotosOutput
    override fun onStartLookingForPhotosObservable(): Observable<Unit> = startLookingForPhotosOutput
    override fun onQueuedUpAndFailedToUploadLoadedObservable(): Observable<List<TakenPhoto>> = onQueuedUpAndFailedToUploadLoadedOutput
    override fun onShowNoUploadedPhotosObservable(): Observable<Unit> = showNoUploadedPhotosOutput
    override fun onBeginReceivingEventsObservable(): Observable<Class<*>> = beginReceivingEventsOutput
    override fun onStopReceivingEventsObservable(): Observable<Class<*>> = stopReceivingEventsOutput
    override fun onPhotoMarkedToBeUploadedObservable(): Observable<Unit> = onPhotoMarkedToBeUploadedOutput

    override fun onAllPhotosUploadedObservable(): Observable<MulticastEvent<Unit>> = allPhotosUploadedOutput
    override fun onStartUploadingPhotosObservable(): Observable<MulticastEvent<Unit>> = startUploadingPhotosOutput
    override fun onShowPhotoUploadedOutputObservable(): Observable<MulticastEvent<TakenPhoto>> = showPhotoUploadedOutput
    override fun onShowFailedToUploadPhotoObservable(): Observable<MulticastEvent<TakenPhoto>> = showFailedToUploadPhotoOutput

    override fun onUnknownErrorObservable(): Observable<Throwable> = unknownErrorSubject
}
















