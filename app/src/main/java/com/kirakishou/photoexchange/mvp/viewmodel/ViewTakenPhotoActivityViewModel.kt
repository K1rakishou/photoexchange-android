package com.kirakishou.photoexchange.mvp.viewmodel

import android.widget.Toast
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.view.ViewTakenPhotoActivityView
import io.reactivex.Completable
import io.reactivex.rxkotlin.plusAssign
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 3/9/2018.
 */
class ViewTakenPhotoActivityViewModel(
    view: WeakReference<ViewTakenPhotoActivityView>,
    private val schedulerProvider: SchedulerProvider,
    private val photosRepository: PhotosRepository
) : BaseViewModel<ViewTakenPhotoActivityView>(view) {

    private val tag = "[${this::class.java.simpleName}] "

    init {
        Timber.tag(tag).e("$tag init")
    }

    override fun onAttached() {
        Timber.tag(tag).d("onAttached()")
    }

    override fun onCleared() {
        Timber.tag(tag).d("onCleared()")

        super.onCleared()
    }

    fun updatePhotoState(takenPhotoId: Long) {
        compositeDisposable += Completable.fromAction {
            try {
                getView()?.hideControls()

                val updateResult = photosRepository.updatePhotoState(takenPhotoId, PhotoState.PHOTO_QUEUED_UP)
                if (!updateResult) {
                    getView()?.showToast("Could not update photo in the database (database error)", Toast.LENGTH_LONG)
                    getView()?.showControls()
                    return@fromAction
                }

                getView()?.onPhotoUpdated()

            } catch (error: Exception) {
                Timber.tag(tag).e(error)
                getView()?.showToast("Could not update photo in the database (database error)", Toast.LENGTH_LONG)
                getView()?.showControls()
            }
        }.subscribeOn(schedulerProvider.BG())
            .observeOn(schedulerProvider.BG())
            .subscribe()
    }
}