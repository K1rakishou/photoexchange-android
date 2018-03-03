package com.kirakishou.photoexchange.ui.widget

import android.support.v7.widget.GridLayoutManager

/**
 * Created by kirakishou on 11/10/2017.
 */
class UploadedPhotosAdapterSpanSizeLookup(
//        private val adapter: UploadedPhotosAdapter,
//        private val columnsCount: Int
) : GridLayoutManager.SpanSizeLookup() {

    override fun getSpanSize(position: Int): Int {
//        val type = adapter.getItemViewType(position)
//        return when (type) {
//            AdapterItemType.VIEW_PROGRESSBAR.ordinal,
//            AdapterItemType.VIEW_MESSAGE.ordinal -> columnsCount
//
//            AdapterItemType.VIEW_ITEM.ordinal,
//            AdapterItemType.VIEW_PHOTO_UPLOADING.ordinal,
//            AdapterItemType.VIEW_QUEUED_UP_PHOTO.ordinal -> 1
//
//            else -> throw RuntimeException("Unknown item view type: $type")
//        }

        return 1
    }
}