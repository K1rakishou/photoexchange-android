package com.kirakishou.photoexchange.ui.widget

import android.support.v7.widget.GridLayoutManager
import com.kirakishou.photoexchange.mwvm.model.other.AdapterItemType
import com.kirakishou.photoexchange.ui.adapter.QueuedUpPhotosAdapter
import com.kirakishou.photoexchange.ui.adapter.UploadedPhotosAdapter

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
            AdapterItemType.VIEW_QUEUED_UP_PHOTO.ordinal -> 1

            else -> throw RuntimeException("Unknown item view type: $type")
        }
    }
}