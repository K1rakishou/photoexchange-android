package com.kirakishou.photoexchange.ui.adapter

import android.support.v7.widget.GridLayoutManager

class GalleryPhotosAdapterSpanSizeLookup(
    private val adapter: GalleryPhotosAdapter,
    private val columnsCount: Int
) : GridLayoutManager.SpanSizeLookup() {

    override fun getSpanSize(position: Int): Int {
        val type = adapter.getItemViewType(position)
        return when (type) {
            AdapterItemType.VIEW_GALLERY_PHOTO.type -> 1
            AdapterItemType.VIEW_PROGRESS.type -> columnsCount

            else -> throw RuntimeException("Unknown item view type: $type")
        }
    }
}