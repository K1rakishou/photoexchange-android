package com.kirakishou.photoexchange.mvp.viewmodel

import android.widget.Toast
import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.view.TakePhotoActivityView
import io.reactivex.Completable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 11/7/2017.
 */
class TakePhotoActivityViewModel(
    view: WeakReference<TakePhotoActivityView>,
    private val coroutinesPool: CoroutineThreadPoolProvider,
    private val photosRepository: PhotosRepository,
    private val settingsRepository: SettingsRepository
) : BaseViewModel<TakePhotoActivityView>(view) {

    private val tag = "[${this::class.java.simpleName}] "

    init {
        Timber.tag(tag).e("$tag init")
    }

    override fun onAttached() {
        Timber.tag(tag).d("onAttached()")

        compositeDisposable += Completable.fromAction {
            photosRepository.init()
        }.subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe()
    }

    override fun onCleared() {
        Timber.tag(tag).d("onCleared()")

        super.onCleared()
    }

    fun takePhoto() {
        compositeDisposable += Completable.fromAction {
            var myPhoto: MyPhoto = MyPhoto.empty()

            try {
                getView()?.hideControls()
                settingsRepository.generateUserIdIfNotExists()

                val file = photosRepository.createFile()
                val takePhotoStatus = getView()?.takePhoto(file)?.blockingGet() ?: false
                if (!takePhotoStatus) {
                    return@fromAction
                }

                myPhoto = photosRepository.saveTakenPhoto(file)
                if (myPhoto.isEmpty()) {
                    photosRepository.deleteMyPhoto(myPhoto)
                    getView()?.showToast("Could not take photo (database error)", Toast.LENGTH_LONG)
                    getView()?.showControls()
                    return@fromAction
                }

                getView()?.onPhotoTaken(myPhoto)
            } catch (error: Throwable) {
                Timber.tag(tag).e(error)
                photosRepository.deleteMyPhoto(myPhoto)
                getView()?.showToast("Could not take photo (database error)", Toast.LENGTH_LONG)
                getView()?.showControls()
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe()
    }
}
















