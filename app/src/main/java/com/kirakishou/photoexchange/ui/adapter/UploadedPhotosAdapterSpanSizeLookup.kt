package com.kirakishou.photoexchange.ui.adapter

import androidx.recyclerview.widget.GridLayoutManager

/**
 * Created by kirakishou on 11/26/2017.
 */
class UploadedPhotosAdapterSpanSizeLookup(
  private val adapter: UploadedPhotosAdapter,
  private val columnsCount: Int
) : GridLayoutManager.SpanSizeLookup() {

  override fun getSpanSize(position: Int): Int {
    val type = adapter.getItemViewType(position)
    return when (type) {
      AdapterItemType.VIEW_TAKEN_PHOTO.type,
      AdapterItemType.VIEW_UPLOADED_PHOTO.type,
      AdapterItemType.VIEW_FAILED_TO_UPLOAD.type -> 1

      AdapterItemType.EMPTY.type,
      AdapterItemType.VIEW_PROGRESS.type,
      AdapterItemType.VIEW_MESSAGE.type -> columnsCount

      else -> throw RuntimeException("Unknown item view type: $type")
    }
  }
}