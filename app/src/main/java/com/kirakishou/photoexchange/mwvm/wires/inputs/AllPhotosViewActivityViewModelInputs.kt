package com.kirakishou.photoexchange.mwvm.wires.inputs

import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer
import com.kirakishou.photoexchange.mwvm.model.other.UploadedPhoto

/**
 * Created by kirakishou on 11/8/2017.
 */
interface AllPhotosViewActivityViewModelInputs {
    fun fetchOnePageUploadedPhotos(page: Int, count: Int)
    fun fetchOnePageReceivedPhotos(page: Int, count: Int)
    fun receivedPhotosFragmentScrollToTop()
    fun receivedPhotosFragmentShowLookingForPhotoIndicator()
    fun uploadedPhotosFragmentShowPhotoUploaded(photo: UploadedPhoto)
    fun uploadedPhotosFragmentShowFailedToUploadPhoto()
    fun receivedPhotosFragmentShowPhotoReceived(photo: PhotoAnswer, allFound: Boolean)
    fun receivedPhotosFragmentShowErrorWhileTryingToLookForPhoto()
    fun receivedPhotosFragmentShowNoPhotoOnServer()
    fun receivedPhotosFragmentShowUserNeedsToUploadMorePhotos()
    fun shouldStartLookingForPhotos()
}