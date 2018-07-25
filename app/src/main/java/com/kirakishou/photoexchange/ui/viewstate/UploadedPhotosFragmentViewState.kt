package com.kirakishou.photoexchange.ui.viewstate

data class UploadedPhotosFragmentViewState(
    var lastId: Long = Long.MAX_VALUE,
    var photosPerPage: Int = 0
) {
    fun update(newLastId: Long? = null, photosPerPage: Int? = null) {
        if (newLastId != null) {
            this.lastId = newLastId
        }

        if (photosPerPage != null) {
            this.photosPerPage = photosPerPage
        }
    }

    fun reset() {
        lastId = Long.MAX_VALUE
    }
}