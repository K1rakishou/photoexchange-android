package com.kirakishou.photoexchange.ui.viewstate

data class UploadedPhotosFragmentViewState(
    var lastId: Long = Long.MAX_VALUE,
    var photosPerPage: Int = 0
) {

    fun updateLastId(newLastId: Long) {
        lastId = newLastId
    }

    fun reset() {
        lastId = Long.MAX_VALUE
    }
}