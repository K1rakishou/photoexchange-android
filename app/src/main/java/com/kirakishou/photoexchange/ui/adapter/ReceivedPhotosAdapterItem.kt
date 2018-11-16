package com.kirakishou.photoexchange.ui.adapter

import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto

sealed class ReceivedPhotosAdapterItem : BaseAdapterItem() {

  override fun getType(): AdapterItemType {
    return when (this) {
      is ReceivedPhotoItem -> AdapterItemType.VIEW_RECEIVED_PHOTO
      is ReceivedPhotosAdapterItem.ProgressItem -> AdapterItemType.VIEW_PROGRESS
      is ReceivedPhotosAdapterItem.MessageItem -> AdapterItemType.VIEW_MESSAGE
    }
  }

  class ReceivedPhotoItem(val receivedPhoto: ReceivedPhoto,
                          var showPhoto: Boolean) : ReceivedPhotosAdapterItem()

  class ProgressItem : ReceivedPhotosAdapterItem()
  class MessageItem(val message: String) : ReceivedPhotosAdapterItem()
}