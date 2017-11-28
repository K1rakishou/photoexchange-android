package com.kirakishou.photoexchange.mwvm.wires.inputs

import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer
import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto

/**
 * Created by kirakishou on 11/8/2017.
 */
interface AllPhotosViewActivityViewModelInputs {
    fun fetchOnePageUploadedPhotos(page: Int, count: Int)
    fun fetchOnePageReceivedPhotos(page: Int, count: Int)
    fun scrollToTop()
    fun showLookingForPhotoIndicator()
    fun startUploadingPhotos(ids: List<Long>)
    fun photoUploaded(photoId: Long)
    fun showFailedToUploadPhoto()
    fun showPhotoReceived(photo: PhotoAnswer, allFound: Boolean)
    fun showErrorWhileTryingToLookForPhoto()
    fun showNoPhotoOnServer()
    fun showUserNeedsToUploadMorePhotos()
    fun shouldStartLookingForPhotos()
    fun allPhotosUploaded()
    fun getQueuedUpPhotos()
    fun cancelTakenPhotoUploading(id: Long)
}