package com.kirakishou.photoexchange.mwvm.viewmodel

import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.util.FileUtils
import com.kirakishou.photoexchange.mwvm.model.other.Constants
import com.kirakishou.photoexchange.mwvm.wires.errors.MainActivityViewModelErrors
import com.kirakishou.photoexchange.mwvm.wires.inputs.MainActivityViewModelInputs
import com.kirakishou.photoexchange.mwvm.wires.outputs.MainActivityViewModelOutputs
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.rx2.await
import timber.log.Timber

/**
 * Created by kirakishou on 11/3/2017.
 */
class TakePhotoActivityViewModel(
        private val uploadedPhotosRepo: UploadedPhotosRepository,
        private val takenPhotosRepo: TakenPhotosRepository,
        private val schedulers: SchedulerProvider
) : BaseViewModel(),
        MainActivityViewModelInputs,
        MainActivityViewModelOutputs,
        MainActivityViewModelErrors {

    val inputs: MainActivityViewModelInputs = this
    val outputs: MainActivityViewModelOutputs = this
    val errors: MainActivityViewModelErrors = this

    private val onUnknownErrorErrorSubject = PublishSubject.create<Throwable>()

    override fun cleanTakenPhotosDB() {
        compositeJob += async {
            try {
                val takenPhotos = takenPhotosRepo.findAll().await()
                FileUtils.deletePhotoFiles(takenPhotos)

                if (Constants.isDebugBuild) {
                    val allPhotos = takenPhotosRepo.findAll().blockingGet()
                    allPhotos.forEach { Timber.d("photo: $it") }
                }
            } catch (error: Throwable) {
                handleErrors(error)
            }
        }
    }

    private fun handleErrors(error: Throwable) {
        Timber.e(error)
        onUnknownErrorErrorSubject.onNext(error)
    }

    override fun onCleared() {
        Timber.d("TakePhotoActivityViewModel.onCleared()")

        super.onCleared()
    }

    override fun onUnknownErrorObservable(): Observable<Throwable> = onUnknownErrorErrorSubject

}