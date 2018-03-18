package com.kirakishou.photoexchange.ui.widget

import android.support.v7.widget.GridLayoutManager
import com.kirakishou.photoexchange.mvp.model.adapter.AdapterItemType
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
            AdapterItemType.VIEW_MY_PHOTO.ordinal -> columnsCount

//            AdapterItemType.VIEW_QUEUED_UP_PHOTO.ordinal,
//            AdapterItemType.VIEW_FAILED_TO_UPLOAD.ordinal -> 1

            else -> throw RuntimeException("Unknown item view type: $type")
        }
    }
}