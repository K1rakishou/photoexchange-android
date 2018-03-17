package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.view.AllPhotosActivityView
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.rx2.awaitSingle
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Created by kirakishou on 3/11/2018.
 */
class AllPhotosActivityViewModel(
    view: AllPhotosActivityView,
    val photosRepository: PhotosRepository,
    val settingsRepository: SettingsRepository,
    val coroutinesPool: CoroutineThreadPoolProvider
): BaseViewModel<AllPhotosActivityView>(view) {

    private val tag = "[${this::class.java.simpleName}] "
    private val LOCATION_CHECK_INTERVAL = TimeUnit.SECONDS.toMillis(5)

    override fun attach() {
    }

    override fun detach() {
        clearView()
        Timber.tag(tag).d("View cleared")
    }

    fun startUploadingPhotosService() {
        async(coroutinesPool.BG()) {
            val count = photosRepository.countAllByState(PhotoState.PHOTO_TO_BE_UPLOADED)
            if (count > 0) {
                view?.startUploadingService()
            }
        }
    }

    fun updateLastLocation() {
        async(coroutinesPool.BG()) {
            val now = TimeUtils.getTimeFast()
            val lastTimeCheck = settingsRepository.findLastLocationCheckTime()
            if (lastTimeCheck == null || (now - lastTimeCheck > LOCATION_CHECK_INTERVAL)) {
                settingsRepository.saveLastLocationCheckTime(now)

                val currentLocation = view?.getCurrentLocation()?.awaitSingle()
                if (currentLocation == null || currentLocation.isEmpty()) {
                    return@async
                }

                settingsRepository.saveLastLocation(currentLocation)
            }
        }
    }

    override fun onCleared() {
        Timber.tag(tag).d("onCleared()")

        super.onCleared()
    }
}