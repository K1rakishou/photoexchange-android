package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.extension.asWeak
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.rx2.asSingle

/**
 * Created by kirakishou on 3/17/2018.
 */
class UploadPhotoServicePresenter(
    private val photosRepository: PhotosRepository,
    private val coroutinePool: CoroutineThreadPoolProvider
) {
    private val tag = "[${this::class.java.simpleName}] "
    private val compositeDisposable = CompositeDisposable()
    private var serviceCallbacks: UploadPhotoServiceCallbacks? = null

    fun onAttach(serviceCallbacks: UploadPhotoServiceCallbacks) {
        this.serviceCallbacks = serviceCallbacks
    }

    fun onDetach() {
        this.serviceCallbacks = null
    }

    fun uploadPhotos(userId: String, location: LonLat) {
        compositeDisposable += async(coroutinePool.BG()) {
            serviceCallbacks?.asWeak()?.let { callback ->
                photosRepository.uploadPhotos(userId, location, callback)
            }

            serviceCallbacks?.stopService()
        }.asSingle(coroutinePool.BG()).subscribe()
    }
}