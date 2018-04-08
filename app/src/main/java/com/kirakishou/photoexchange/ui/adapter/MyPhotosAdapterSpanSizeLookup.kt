package com.kirakishou.photoexchange.ui.adapter

import android.support.v7.widget.GridLayoutManager
import com.kirakishou.photoexchange.ui.adapter.AdapterItemType
import com.kirakishou.photoexchange.ui.adapter.MyPhotosAdapter

/**
 * Created by kirakishou on 11/26/2017.
 */
class MyPhotosAdapterSpanSizeLookup(
    private val adapter: MyPhotosAdapter,
    private val columnsCount: Int
) : GridLayoutManager.SpanSizeLookup() {

    override fun getSpanSize(position: Int): Int {
        val type = adapter.getItemViewType(position)
        return when (type) {
            AdapterItemType.VIEW_MY_PHOTO.type -> 1

            AdapterItemType.EMPTY.type,
            AdapterItemType.VIEW_PROGRESS.type,
            AdapterItemType.VIEW_OBTAIN_CURRENT_LOCATION_NOTIFICATION.type,
            AdapterItemType.VIEW_FAILED_TO_UPLOAD.type -> columnsCount

            else -> throw RuntimeException("Unknown item view type: $type")
        }
    }
}