package com.kirakishou.photoexchange.ui.widget

import android.support.v7.widget.GridLayoutManager
import com.kirakishou.photoexchange.mwvm.model.adapter.AdapterItemType
import com.kirakishou.photoexchange.ui.adapter.QueuedUpPhotosAdapter

/**
 * Created by kirakishou on 11/26/2017.
 */
class QueuedUpPhotosAdapterSpanSizeLookup(
        private val adapter: QueuedUpPhotosAdapter,
        private val columnsCount: Int
) : GridLayoutManager.SpanSizeLookup() {

    override fun getSpanSize(position: Int): Int {
        val type = adapter.getItemViewType(position)
        return when (type) {
            AdapterItemType.VIEW_MESSAGE.ordinal -> columnsCount

            AdapterItemType.VIEW_QUEUED_UP_PHOTO.ordinal,
            AdapterItemType.VIEW_FAILED_TO_UPLOAD.ordinal -> 1

            else -> throw RuntimeException("Unknown item view type: $type")
        }
    }
}