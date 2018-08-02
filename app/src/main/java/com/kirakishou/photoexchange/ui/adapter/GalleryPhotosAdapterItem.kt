package com.kirakishou.photoexchange.ui.adapter

import com.kirakishou.photoexchange.mvp.model.GalleryPhoto

sealed class GalleryPhotosAdapterItem : BaseAdapterItem() {

    override fun getType(): AdapterItemType {
        return when (this) {
            is GalleryPhotoItem -> AdapterItemType.VIEW_GALLERY_PHOTO
            is GalleryPhotosAdapterItem.ProgressItem -> AdapterItemType.VIEW_PROGRESS
        }
    }

    class GalleryPhotoItem(val photo: GalleryPhoto,
                           var showPhoto: Boolean) : GalleryPhotosAdapterItem()
    class ProgressItem : GalleryPhotosAdapterItem()
}