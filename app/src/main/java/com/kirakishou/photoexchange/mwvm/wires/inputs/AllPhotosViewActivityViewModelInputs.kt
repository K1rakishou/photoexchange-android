package com.kirakishou.photoexchange.mwvm.wires.inputs

import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer
import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto

/**
 * Created by kirakishou on 11/8/2017.
 */
interface AllPhotosViewActivityViewModelInputs {
    fun beginReceivingEvents(clazz: Class<*>)
    fun stopReceivingEvents(clazz: Class<*>)
    fun fetchOnePageUploadedPhotos(page: Int, count: Int)
    fun fetchOnePageReceivedPhotos(page: Int, count: Int)
    fun scrollToTop()
    fun showLookingForPhotoIndicator()
    fun showPhotoReceived(photo: PhotoAnswer, allFound: Boolean)
    fun showErrorWhileTryingToLookForPhoto()
    fun showNoPhotoOnServer()
    fun showUserNeedsToUploadMorePhotos()
    fun getQueuedUpAndFailedToUploadPhotos()
    fun cancelTakenPhotoUploading(id: Long)
    fun startLookingForPhotos()
    fun startPhotosUploading()

    fun preparePhotosUploading(receiver: Class<*>)
    fun photoUploaded(receiver: Class<*>, photo: TakenPhoto)
    fun showFailedToUploadPhoto(receiver: Class<*>, photo: TakenPhoto)
    fun allPhotosUploaded(receiver: Class<*>)
}