package com.kirakishou.photoexchange.ui.adapter

import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto

sealed class MyPhotosAdapterItem : BaseAdapterItem() {

    override fun getType(): AdapterItemType {
        return when (this) {
            is EmptyItem -> AdapterItemType.EMPTY
            is TakenPhotoItem -> AdapterItemType.VIEW_TAKEN_PHOTO
            is UploadedPhotoItem -> AdapterItemType.VIEW_PROGRESS
            is ProgressItem -> AdapterItemType.VIEW_PROGRESS
            is ObtainCurrentLocationItem -> AdapterItemType.VIEW_OBTAIN_CURRENT_LOCATION_NOTIFICATION
            is FailedToUploadItem -> AdapterItemType.VIEW_FAILED_TO_UPLOAD
        }
    }

    class EmptyItem : MyPhotosAdapterItem()
    class TakenPhotoItem(val takenPhoto: TakenPhoto) : MyPhotosAdapterItem()
    class UploadedPhotoItem(val uploadedPhoto: UploadedPhoto) : MyPhotosAdapterItem()
    class ProgressItem : MyPhotosAdapterItem()
    class FailedToUploadItem(val failedToUploadPhoto: TakenPhoto) : MyPhotosAdapterItem()
    class ObtainCurrentLocationItem : MyPhotosAdapterItem()
}