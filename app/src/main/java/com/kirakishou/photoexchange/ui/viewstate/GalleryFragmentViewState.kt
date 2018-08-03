package com.kirakishou.photoexchange.ui.viewstate

class GalleryFragmentViewState(
    var lastId: Long = Long.MAX_VALUE
) {

    fun updateLastId(lastId: Long) {
        this.lastId = lastId
    }
}