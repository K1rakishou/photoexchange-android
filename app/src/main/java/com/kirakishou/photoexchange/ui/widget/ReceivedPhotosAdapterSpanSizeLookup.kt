package com.kirakishou.photoexchange.ui.widget

import android.support.v7.widget.GridLayoutManager
import com.kirakishou.photoexchange.mwvm.model.other.AdapterItemType
import com.kirakishou.photoexchange.ui.adapter.ReceivedPhotosAdapter
import com.kirakishou.photoexchange.ui.adapter.TakenPhotosAdapter

/**
 * Created by kirakishou on 11/17/2017.
 */
class ReceivedPhotosAdapterSpanSizeLookup(
        private val adapter: ReceivedPhotosAdapter,
        private val columnsCount: Int
) : GridLayoutManager.SpanSizeLookup() {

    override fun getSpanSize(position: Int): Int {
        val type = adapter.getItemViewType(position)
        return when (type) {
            AdapterItemType.VIEW_PROGRESSBAR.ordinal -> columnsCount
            AdapterItemType.VIEW_ITEM.ordinal-> 1

            else -> throw RuntimeException("Unknown item view type: $type")
        }
    }
}