package com.kirakishou.photoexchange.ui.adapter

import com.kirakishou.photoexchange.mvp.model.MyPhoto

sealed class MyPhotosAdapterItem : BaseAdapterItem() {

    override fun getType(): AdapterItemType {
        return when (this) {
            is MyPhotosAdapterItem.EmptyItem -> AdapterItemType.EMPTY
            is MyPhotosAdapterItem.MyPhotoItem -> AdapterItemType.VIEW_MY_PHOTO
            is MyPhotosAdapterItem.ProgressItem -> AdapterItemType.VIEW_PROGRESS
            is MyPhotosAdapterItem.QueuedUpPhotosItem -> AdapterItemType.VIEW_QUEUED_UP_PHOTOS_NOTIFICATION
            is MyPhotosAdapterItem.ObtainCurrentLocationItem -> AdapterItemType.VIEW_OBTAIN_CURRENT_LOCATION_NOTIFICATION
            is MyPhotosAdapterItem.FailedToUploadItem -> AdapterItemType.VIEW_FAILED_TO_UPLOAD_PHOTOS_NOTIFICATION
        }
    }

    class EmptyItem : MyPhotosAdapterItem()
    class MyPhotoItem(val myPhoto: MyPhoto) : MyPhotosAdapterItem()
    class ProgressItem : MyPhotosAdapterItem()
    class QueuedUpPhotosItem(var count: Int) : MyPhotosAdapterItem()
    class ObtainCurrentLocationItem : MyPhotosAdapterItem()
    class FailedToUploadItem(var count: Int) : MyPhotosAdapterItem()
}