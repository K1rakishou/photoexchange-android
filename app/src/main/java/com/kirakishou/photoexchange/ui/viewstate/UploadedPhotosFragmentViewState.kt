package com.kirakishou.photoexchange.ui.viewstate

import android.os.Bundle

data class UploadedPhotosFragmentViewState(
    var lastId: Long = Long.MAX_VALUE,
    var photosPerPage: Int = 0
) : BaseViewState {

    fun updateLastId(newLastId: Long) {
        lastId = newLastId
    }

    override fun saveToBundle(bundle: Bundle?) {
    }

    override fun loadFromBundle(bundle: Bundle?) {
    }

}