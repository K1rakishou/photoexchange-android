package com.kirakishou.photoexchange.ui.adapter

import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto

sealed class ReceivedPhotosAdapterItem : BaseAdapterItem() {

    override fun getType(): AdapterItemType {
        return when (this) {
            is ReceivedPhotoItem -> AdapterItemType.VIEW_RECEIVED_PHOTO
        }
    }

    class ReceivedPhotoItem(val receivedPhoto: ReceivedPhoto) : ReceivedPhotosAdapterItem()
}