package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.*
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.helper.intercom.event.GalleryFragmentEvent
import com.kirakishou.photoexchange.helper.intercom.event.ReceivedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.interactors.*
import com.kirakishou.photoexchange.mvp.model.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.ui.fragment.GalleryFragment
import com.kirakishou.photoexchange.ui.fragment.ReceivedPhotosFragment
import com.kirakishou.photoexchange.ui.fragment.UploadedPhotosFragment
import io.reactivex.Completable
import io.reactivex.Observable
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Created by kirakishou on 3/11/2018.
 */
class PhotosActivityViewModel(
    private val settingsRepository: SettingsRepository,
    private val takenPhotosRepository: TakenPhotosRepository,
    private val uploadedPhotosRepository: UploadedPhotosRepository,
    private val receivedPhotosRepository: ReceivedPhotosRepository,
    val uploadedPhotosFragmentViewModel: UploadedPhotosFragmentViewModel,
    val receivedPhotosFragmentViewModel: ReceivedPhotosFragmentViewModel,
    val galleryFragmentViewModel: GalleryFragmentViewModel,
    private val reportPhotoUseCase: ReportPhotoUseCase,
    private val favouritePhotoUseCase: FavouritePhotoUseCase,
    private val schedulerProvider: SchedulerProvider
) : BaseViewModel() {

    private val TAG = "PhotosActivityViewModel"

    val intercom = PhotosActivityViewModelIntercom()

    init {
        uploadedPhotosFragmentViewModel.intercom = intercom
        receivedPhotosFragmentViewModel.intercom = intercom
        galleryFragmentViewModel.intercom = intercom
    }

    override fun onCleared() {
        Timber.tag(TAG).d("onCleared()")

        uploadedPhotosFragmentViewModel.onCleared()
        receivedPhotosFragmentViewModel.onCleared()
        galleryFragmentViewModel.onCleared()

        super.onCleared()
    }

    fun reportPhoto(photoName: String): Observable<Either<ErrorCode.ReportPhotoErrors, Boolean>> {
        return Observable.fromCallable { settingsRepository.getUserId() }
            .subscribeOn(schedulerProvider.IO())
            .concatMap { userId -> reportPhotoUseCase.reportPhoto(userId, photoName) }
    }

    fun favouritePhoto(photoName: String): Observable<Either<ErrorCode.FavouritePhotoErrors, FavouritePhotoUseCase.FavouritePhotoResult>> {
        return Observable.fromCallable { settingsRepository.getUserId() }
            .subscribeOn(schedulerProvider.IO())
            .concatMap { userId -> favouritePhotoUseCase.favouritePhoto(userId, photoName) }
    }

    fun checkHasPhotosToUpload(): Observable<Boolean> {
        return Observable.fromCallable {
            return@fromCallable takenPhotosRepository.countAllByState(PhotoState.PHOTO_QUEUED_UP) > 0
        }.subscribeOn(schedulerProvider.IO())
    }

    fun checkHasPhotosToReceive(): Observable<Boolean> {
        return Observable.fromCallable {
            val uploadedPhotosCount = uploadedPhotosRepository.count()
            val receivedPhotosCount = receivedPhotosRepository.count()

            return@fromCallable uploadedPhotosCount > receivedPhotosCount
        }.subscribeOn(schedulerProvider.IO())
    }

    fun deletePhotoById(photoId: Long): Completable {
        return Completable.fromAction {
            takenPhotosRepository.deletePhotoById(photoId)
            if (Constants.isDebugBuild) {
                check(takenPhotosRepository.findById(photoId).isEmpty())
            }
        }.subscribeOn(schedulerProvider.IO())
    }

    fun changePhotoState(photoId: Long, newPhotoState: PhotoState): Completable {
        return Completable.fromAction {
            takenPhotosRepository.updatePhotoState(photoId, newPhotoState)
        }.subscribeOn(schedulerProvider.IO())
    }

    fun updateGpsPermissionGranted(granted: Boolean): Completable {
        return Completable.fromAction {
            settingsRepository.updateGpsPermissionGranted(granted)
        }.subscribeOn(schedulerProvider.IO())
    }
}
