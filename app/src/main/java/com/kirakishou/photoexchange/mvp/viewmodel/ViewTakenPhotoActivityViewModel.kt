package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.view.ViewTakenPhotoActivityView
import com.kirakishou.photoexchange.ui.fragment.AddToGalleryDialogFragment
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

/**
 * Created by kirakishou on 3/9/2018.
 */
class ViewTakenPhotoActivityViewModel(
    private val schedulerProvider: SchedulerProvider,
    private val photosRepository: PhotosRepository,
    private val settingsRepository: SettingsRepository
) : BaseViewModel<ViewTakenPhotoActivityView>() {

    private val tag = "[${this::class.java.simpleName}] "

    val addToGalleryFragmentResult = PublishSubject.create<AddToGalleryDialogFragment.FragmentResult>().toSerialized()

    override fun onCleared() {
        Timber.tag(tag).d("onCleared()")

        super.onCleared()
    }

    fun queueUpTakenPhoto(takenPhotoId: Long): Observable<Boolean> {
        return Observable
            .fromCallable { photosRepository.updatePhotoState(takenPhotoId, PhotoState.PHOTO_QUEUED_UP) }
            .subscribeOn(schedulerProvider.IO())
    }

    fun makePhotoPublic(takenPhotoId: Long): Observable<Boolean> {
        return Observable
            .fromCallable { photosRepository.updateMakePhotoPublic(takenPhotoId) }
            .subscribeOn(schedulerProvider.IO())
    }

    fun saveMakePublicFlag(makePublic: Boolean) {
        settingsRepository.saveMakePublicFlag(makePublic)
    }

    fun getMakePublicFlag(): SettingsRepository.MakePhotosPublicState {
        return settingsRepository.getMakePublicFlag()
    }
}