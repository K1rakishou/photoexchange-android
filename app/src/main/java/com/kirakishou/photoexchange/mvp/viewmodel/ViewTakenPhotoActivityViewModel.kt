package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.view.ViewTakenPhotoActivityView
import com.kirakishou.photoexchange.ui.fragment.AddToGalleryDialogFragment
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

/**
 * Created by kirakishou on 3/9/2018.
 */
class ViewTakenPhotoActivityViewModel(
    private val schedulerProvider: SchedulerProvider,
    private val takenPhotosRepository: TakenPhotosRepository,
    private val settingsRepository: SettingsRepository
) : BaseViewModel<ViewTakenPhotoActivityView>() {

    private val TAG = "ViewTakenPhotoActivityViewModel"

    val addToGalleryFragmentResult = PublishSubject.create<AddToGalleryDialogFragment.FragmentResult>().toSerialized()

    override fun onCleared() {
        Timber.tag(TAG).d("onCleared()")

        super.onCleared()
    }

    fun queueUpTakenPhoto(takenPhotoId: Long): Observable<Boolean> {
        return Observable
            .fromCallable { takenPhotosRepository.updatePhotoState(takenPhotoId, PhotoState.PHOTO_QUEUED_UP) }
            .subscribeOn(schedulerProvider.IO())
            .doOnError { Timber.tag(TAG).e(it) }
    }

    fun updateSetIsPhotoPublic(takenPhotoId: Long): Observable<Boolean> {
        return Observable
            .fromCallable { takenPhotosRepository.updateMakePhotoPublic(takenPhotoId) }
            .subscribeOn(schedulerProvider.IO())
            .doOnError { Timber.tag(TAG).e(it) }
    }

    fun saveMakePublicFlag(rememberChoice: Boolean, makePublic: Boolean): Completable {
        return Completable.fromAction {
            if (!rememberChoice) {
                return@fromAction
            }

            settingsRepository.saveMakePublicFlag(makePublic)
        }.doOnError { Timber.tag(TAG).e(it) }
    }

    fun getMakePublicFlag(): Observable<SettingsRepository.MakePhotosPublicState> {
        return Observable.fromCallable { settingsRepository.getMakePublicFlag() }
            .subscribeOn(schedulerProvider.IO())
            .doOnError { Timber.tag(TAG).e(it) }
    }
}