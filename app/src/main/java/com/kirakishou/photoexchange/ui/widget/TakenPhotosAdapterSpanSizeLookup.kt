package com.kirakishou.photoexchange.ui.widget

import android.support.v7.widget.GridLayoutManager
import com.kirakishou.photoexchange.mvvm.model.other.AdapterItemType
import com.kirakishou.photoexchange.ui.adapter.TakenPhotosAdapter

/**
 * Created by kirakishou on 11/10/2017.
 */
class TakenPhotosAdapterSpanSizeLookup(
        private val adapter: TakenPhotosAdapter,
        private val columnsCount: Int
) : GridLayoutManager.SpanSizeLookup() {

    override fun getSpanSize(position: Int): Int {
        val type = adapter.getItemViewType(position)
        return when (type) {
            AdapterItemType.VIEW_PROGRESSBAR.ordinal,
            AdapterItemType.VIEW_MESSAGE.ordinal -> columnsCount

            AdapterItemType.VIEW_ITEM.ordinal,
            AdapterItemType.VIEW_FAILED_TO_UPLOAD.ordinal -> 1

            else -> throw RuntimeException("Unknown item view type: $type")
        }
    }
}