package com.kirakishou.photoexchange.ui.widget

import android.support.v7.widget.GridLayoutManager

/**
 * Created by kirakishou on 11/17/2017.
 */
class ReceivedPhotosAdapterSpanSizeLookup(
//        private val adapter: ReceivedPhotosAdapter,
//        private val columnsCount: Int
) : GridLayoutManager.SpanSizeLookup() {

    override fun getSpanSize(position: Int): Int {
//        val type = adapter.getItemViewType(position)
//        return when (type) {
//            AdapterItemType.VIEW_PROGRESSBAR.ordinal,
//            AdapterItemType.VIEW_MESSAGE.ordinal -> columnsCount
//
//            AdapterItemType.VIEW_LOOKING_FOR_PHOTO.ordinal,
//            AdapterItemType.VIEW_ITEM.ordinal -> 1
//
//            else -> throw RuntimeException("Unknown item view type: $type")
//        }

        return 1
    }
}