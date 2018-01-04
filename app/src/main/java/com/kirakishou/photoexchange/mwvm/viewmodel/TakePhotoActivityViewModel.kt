package com.kirakishou.photoexchange.mwvm.viewmodel

import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.database.repository.PhotoAnswerRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.util.FileUtils
import com.kirakishou.photoexchange.mwvm.model.other.Constants
import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto
import com.kirakishou.photoexchange.mwvm.model.state.PhotoState
import com.kirakishou.photoexchange.mwvm.wires.errors.MainActivityViewModelErrors
import com.kirakishou.photoexchange.mwvm.wires.inputs.MainActivityViewModelInputs
import com.kirakishou.photoexchange.mwvm.wires.outputs.MainActivityViewModelOutputs
import io.reactivex.Observable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.rx2.asCompletable
import kotlinx.coroutines.experimental.rx2.await
import timber.log.Timber

/**
 * Created by kirakishou on 11/3/2017.
 */
class TakePhotoActivityViewModel(
        private val takenPhotosRepo: TakenPhotosRepository,
        private val photoAnswerRepo: PhotoAnswerRepository,
        private val schedulers: SchedulerProvider
) : BaseViewModel(),
        MainActivityViewModelInputs,
        MainActivityViewModelOutputs,
        MainActivityViewModelErrors {

    private val tag = "[${this::class.java.simpleName}]: "

    val inputs: MainActivityViewModelInputs = this
    val outputs: MainActivityViewModelOutputs = this
    val errors: MainActivityViewModelErrors = this

    private val onTakenPhotoSavedOutput = PublishSubject.create<TakenPhoto>()
    private val onUnknownErrorErrorSubject = PublishSubject.create<Throwable>()

    fun showDatabaseDebugInfo() {
        if (!Constants.isDebugBuild) {
            return
        }

        compositeDisposable += async {
            //we don't care about concurrency here
            val allTakenPhotos = takenPhotosRepo.findAllDebug().await()
            val allPhotoAnswers = photoAnswerRepo.findAllDebug().await()

            Timber.tag(tag).d("showDatabaseDebugInfo() === Taken photos ===")
            allTakenPhotos.forEach { Timber.tag(tag).d("showDatabaseDebugInfo() photo: $it") }

            Timber.tag(tag).d("showDatabaseDebugInfo() === Photo answer ===")
            allPhotoAnswers.forEach { Timber.tag(tag).d("showDatabaseDebugInfo() photo: $it") }
        }.asCompletable(CommonPool).subscribe()
    }

    override fun cleanTakenPhotosDB() {
        compositeDisposable += async {
            try {
                val takenPhotos = takenPhotosRepo.findAllTaken().await()
                FileUtils.deletePhotosFiles(takenPhotos)

                takenPhotosRepo.deleteManyById(takenPhotos.map { it.id }).await()
                printTakenPhotoTable()
            } catch (error: Throwable) {
                handleErrors(error)
            }
        }.asCompletable(CommonPool).subscribe()
    }

    override fun saveTakenPhoto(takenPhoto: TakenPhoto) {
        compositeDisposable += async {
            try {
                val id = takenPhotosRepo.saveOne(takenPhoto.location.lon, takenPhoto.location.lat,
                        takenPhoto.photoFilePath, takenPhoto.userId, PhotoState.TAKEN).await()
                val savedTakenPhoto = takenPhoto.copy(id)

                printTakenPhotoTable()
                onTakenPhotoSavedOutput.onNext(savedTakenPhoto)
            } catch (error: Throwable) {
                onTakenPhotoSavedOutput.onError(error)
            }
        }.asCompletable(CommonPool).subscribe()
    }

    private suspend fun printTakenPhotoTable() {
        if (Constants.isDebugBuild) {
            val allPhotos = takenPhotosRepo.findAll().await()
            allPhotos.forEach { Timber.tag(tag).d("printTakenPhotoTable() photo: $it") }
        }
    }

    private fun handleErrors(error: Throwable) {
        Timber.e(error)
        onUnknownErrorErrorSubject.onNext(error)
    }

    override fun onCleared() {
        Timber.tag(tag).d("onCleared()")

        super.onCleared()
    }

    override fun onTakenPhotoSavedObservable(): Observable<TakenPhoto> = onTakenPhotoSavedOutput
    override fun onUnknownErrorObservable(): Observable<Throwable> = onUnknownErrorErrorSubject
}