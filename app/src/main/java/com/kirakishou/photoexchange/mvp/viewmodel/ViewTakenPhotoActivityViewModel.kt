package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.view.ViewTakenPhotoActivityView
import io.reactivex.Observable
import timber.log.Timber

/**
 * Created by kirakishou on 3/9/2018.
 */
class ViewTakenPhotoActivityViewModel(
    private val schedulerProvider: SchedulerProvider,
    private val photosRepository: PhotosRepository
) : BaseViewModel<ViewTakenPhotoActivityView>() {

    private val tag = "[${this::class.java.simpleName}] "

    override fun onCleared() {
        Timber.tag(tag).d("onCleared()")

        super.onCleared()
    }

    fun updatePhotoState(takenPhotoId: Long): Observable<Boolean> {
        return Observable.fromCallable {
            photosRepository.updatePhotoState(takenPhotoId, PhotoState.PHOTO_QUEUED_UP)
        }.subscribeOn(schedulerProvider.IO())
    }
}