package com.kirakishou.photoexchange.ui.adapter

import com.kirakishou.photoexchange.mvp.model.MyPhoto

sealed class MyPhotosAdapterItem : BaseAdapterItem() {

    override fun getType(): AdapterItemType {
        return when (this) {
            is MyPhotosAdapterItem.EmptyItem -> AdapterItemType.EMPTY
            is MyPhotosAdapterItem.MyPhotoItem -> AdapterItemType.VIEW_MY_PHOTO
            is MyPhotosAdapterItem.ProgressItem -> AdapterItemType.VIEW_PROGRESS
            is MyPhotosAdapterItem.ObtainCurrentLocationItem -> AdapterItemType.VIEW_OBTAIN_CURRENT_LOCATION_NOTIFICATION
        }
    }

    class EmptyItem : MyPhotosAdapterItem()
    class MyPhotoItem(val myPhoto: MyPhoto) : MyPhotosAdapterItem()
    class ProgressItem : MyPhotosAdapterItem()
    class ObtainCurrentLocationItem : MyPhotosAdapterItem()
}