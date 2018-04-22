package com.kirakishou.photoexchange.ui.adapter

import android.support.v7.widget.GridLayoutManager

class ReceivedPhotosAdapterSpanSizeLookup(
    private val adapter: ReceivedPhotosAdapter,
    private val columnsCount: Int
) : GridLayoutManager.SpanSizeLookup() {

    override fun getSpanSize(position: Int): Int {
        val type = adapter.getItemViewType(position)

        return when (type) {
            AdapterItemType.VIEW_RECEIVED_PHOTO.type -> columnsCount

            else -> throw RuntimeException("Unknown item view type: $type")
        }
    }
}