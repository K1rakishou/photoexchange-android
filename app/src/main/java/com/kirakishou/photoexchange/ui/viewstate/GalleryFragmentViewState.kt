package com.kirakishou.photoexchange.ui.viewstate

class GalleryFragmentViewState(
    var lastId: Long = Long.MAX_VALUE,
    var photosPerPage: Int = 0
) {

    fun updateLastId(lastId: Long) {
        this.lastId = lastId
    }

    fun reset() {
        lastId = Long.MAX_VALUE
    }
}